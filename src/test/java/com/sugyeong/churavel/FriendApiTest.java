package com.sugyeong.churavel;

import com.jayway.jsonpath.JsonPath;
import com.sugyeong.churavel.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 여권(나라 체크인) · 팔로우 요청/수락 · 친구 지도 보기 · 댓글 */
class FriendApiTest extends ApiTestSupport {

    User mina;   // 지도 주인
    User jun;    // 팔로우하는 친구
    User stranger;

    @BeforeEach
    void setUp() {
        mina = user(2001L, "미나");
        jun = user(2002L, "준");
        stranger = user(2003L, "낯선이");
    }

    private String checkIn(User u, String code) throws Exception {
        String body = mvc.perform(multipart(HttpMethod.POST, "/api/visits/{code}/entries", code).file(jpeg(code + ".jpg")).with(as(u)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return body;
    }

    private void follow(User from, User to) throws Exception {
        mvc.perform(post("/api/friends/{no}/follow", to.getPassportNo()).with(as(from))).andExpect(status().isOk());
        mvc.perform(post("/api/friends/{no}/accept", from.getPassportNo()).with(as(to))).andExpect(status().isNoContent());
    }

    @Test
    void 나라_단위로_체크인한다() throws Exception {
        mvc.perform(multipart(HttpMethod.POST, "/api/visits/JP/entries").file(jpeg("tokyo.jpg")).with(as(mina)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regionName").value("일본"))
                .andExpect(jsonPath("$.country").value("JP"))
                .andExpect(jsonPath("$.id").isNumber());
        // 다른 나라 행정구역 (도쿄도)
        mvc.perform(multipart(HttpMethod.POST, "/api/visits/JPN-1860/entries").file(jpeg("shibuya.jpg")).with(as(mina)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regionName").value("도쿄도"))
                .andExpect(jsonPath("$.country").value("JP"));
        // 한국 시·군·구는 country = KR
        mvc.perform(multipart(HttpMethod.POST, "/api/visits/3203/entries").file(jpeg("gn.jpg")).with(as(mina)))
                .andExpect(jsonPath("$.country").value("KR"));
        // 한국은 나라 하나로 체크인하지 않는다
        mvc.perform(multipart(HttpMethod.POST, "/api/visits/KR/entries").file(jpeg("kr.jpg")).with(as(mina)))
                .andExpect(status().isNotFound());
    }

    @Test
    void 내_여권() throws Exception {
        mvc.perform(get("/api/me").with(as(mina)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passportNo").value(mina.getPassportNo()))
                .andExpect(jsonPath("$.relation").value("SELF"))
                .andExpect(jsonPath("$.issuedOn").exists());
    }

    @Test
    void 팔로우_요청이_수락되어야_친구_지도를_본다() throws Exception {
        String visit = checkIn(mina, "1101");
        String photoUrl = JsonPath.read(visit, "$.photoUrl");
        String no = mina.getPassportNo();

        // 요청 전: 여권 표지는 보이지만 지도·사진은 못 본다
        mvc.perform(get("/api/travelers/{no}", no).with(as(jun)))
                .andExpect(jsonPath("$.nickname").value("미나"))
                .andExpect(jsonPath("$.relation").value("NONE"))
                .andExpect(jsonPath("$.canView").value(false));
        mvc.perform(get("/api/travelers/{no}/visits", no).with(as(jun)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("팔로우 요청이 수락되면 볼 수 있어요"));
        mvc.perform(get(photoUrl).with(as(jun))).andExpect(status().isNotFound());

        // 요청 → 아직 못 본다
        mvc.perform(post("/api/friends/{no}/follow", no).with(as(jun)))
                .andExpect(jsonPath("$.relation").value("REQUESTED"));
        mvc.perform(get("/api/travelers/{no}/visits", no).with(as(jun))).andExpect(status().isForbidden());
        mvc.perform(get("/api/friends").with(as(mina)))
                .andExpect(jsonPath("$.incoming[0].passportNo").value(jun.getPassportNo()));

        // 수락 → 본다
        mvc.perform(post("/api/friends/{no}/accept", jun.getPassportNo()).with(as(mina))).andExpect(status().isNoContent());
        mvc.perform(get("/api/travelers/{no}", no).with(as(jun)))
                .andExpect(jsonPath("$.relation").value("FOLLOWING"))
                .andExpect(jsonPath("$.canView").value(true));
        mvc.perform(get("/api/travelers/{no}/visits", no).with(as(jun)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].regionCode").value("1101"));
        mvc.perform(get(photoUrl).with(as(jun))).andExpect(status().isOk());
        mvc.perform(get("/api/travelers/{no}/visits/{code}", no, "1101").with(as(jun)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries.length()").value(1));
        mvc.perform(get("/api/friends").with(as(jun)))
                .andExpect(jsonPath("$.following[0].nickname").value("미나"));

        // 한 방향: 미나는 준의 지도를 못 본다
        mvc.perform(get("/api/travelers/{no}/visits", jun.getPassportNo()).with(as(mina))).andExpect(status().isForbidden());

        // 팔로워 끊기 → 다시 못 본다
        mvc.perform(delete("/api/friends/{no}/follower", jun.getPassportNo()).with(as(mina))).andExpect(status().isNoContent());
        mvc.perform(get("/api/travelers/{no}/visits", no).with(as(jun))).andExpect(status().isForbidden());
        mvc.perform(get("/api/travelers/{no}/visits/{code}", no, "1101").with(as(jun))).andExpect(status().isForbidden());
    }

    @Test
    void 나_자신은_팔로우할_수_없다() throws Exception {
        mvc.perform(post("/api/friends/{no}/follow", mina.getPassportNo()).with(as(mina)))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/friends/{no}/follow", "M99999999").with(as(mina)))
                .andExpect(status().isNotFound());
    }

    @Test
    void 팔로워와_주인만_댓글을_쓰고_지운다() throws Exception {
        checkIn(mina, "TH");
        String visit = checkIn(mina, "TH"); // 기록 두 개: 댓글은 기록마다 따로
        Integer entryId = JsonPath.read(visit, "$.entries[0].id");
        Integer otherEntry = JsonPath.read(visit, "$.entries[1].id");
        follow(jun, mina);

        // 낯선 사람은 못 읽고 못 쓴다
        mvc.perform(get("/api/comments").param("entryId", entryId.toString()).with(as(stranger)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/comments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"entryId\":" + entryId + ",\"body\":\"안녕\"}").with(as(stranger)))
                .andExpect(status().isForbidden());

        // 팔로워가 쓴다
        String c1 = mvc.perform(post("/api/comments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"entryId\":" + entryId + ",\"body\":\"방콕 좋았겠다!\"}").with(as(jun)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.author.nickname").value("준"))
                .andExpect(jsonPath("$.deletable").value(true))
                .andReturn().getResponse().getContentAsString();
        // 주인이 쓴다
        mvc.perform(post("/api/comments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"entryId\":" + entryId + ",\"body\":\"응 최고\"}").with(as(mina)))
                .andExpect(status().isOk());

        // 준에게는 자기 댓글만 지울 수 있게 보인다
        mvc.perform(get("/api/comments").param("entryId", entryId.toString()).with(as(jun)))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].deletable").value(true))
                .andExpect(jsonPath("$[1].deletable").value(false));
        mvc.perform(get("/api/visits/TH").with(as(mina)))
                .andExpect(jsonPath("$.commentCount").value(2))
                .andExpect(jsonPath("$.entries[0].commentCount").value(2))
                .andExpect(jsonPath("$.entries[1].commentCount").value(0));
        mvc.perform(get("/api/comments").param("entryId", otherEntry.toString()).with(as(jun)))
                .andExpect(jsonPath("$.length()").value(0));

        // 빈 댓글은 안 된다
        mvc.perform(post("/api/comments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"entryId\":" + entryId + ",\"body\":\"  \"}").with(as(jun)))
                .andExpect(status().isBadRequest());

        // 준은 미나의 댓글을 못 지우고, 미나는 준의 댓글도 지울 수 있다 (내 지도)
        Integer minaComment = JsonPath.read(mvc.perform(get("/api/comments").param("entryId", entryId.toString()).with(as(mina)))
                .andReturn().getResponse().getContentAsString(), "$[1].id");
        mvc.perform(delete("/api/comments/{id}", minaComment).with(as(jun))).andExpect(status().isForbidden());
        Integer junComment = JsonPath.read(c1, "$.id");
        mvc.perform(delete("/api/comments/{id}", junComment).with(as(mina))).andExpect(status().isNoContent());

        // 체크인을 지우면 기록·댓글도 같이 지워진다
        mvc.perform(delete("/api/visits/TH").with(as(mina))).andExpect(status().isNoContent());
        mvc.perform(get("/api/comments").param("entryId", entryId.toString()).with(as(mina)))
                .andExpect(status().isNotFound());
    }
}
