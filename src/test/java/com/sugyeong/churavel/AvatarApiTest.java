package com.sugyeong.churavel;

import com.jayway.jsonpath.JsonPath;
import com.sugyeong.churavel.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 프로필 사진 바꾸기·원래대로, 누가 볼 수 있는지 */
class AvatarApiTest extends ApiTestSupport {

    private String change(User u) throws Exception {
        String body = mvc.perform(multipart(HttpMethod.PUT, "/api/me/photo").file(jpeg("me.jpg")).with(as(u)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileImageUrl").value(startsWith("/api/avatars/")))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.profileImageUrl");
    }

    @Test
    void 프로필_사진을_바꾸면_모두에게_보인다() throws Exception {
        User mina = user(5001L, "미나");
        User stranger = user(5002L, "낯선이");
        String url = change(mina);

        mvc.perform(get("/api/me").with(as(mina))).andExpect(jsonPath("$.profileImageUrl").value(url));
        // 팔로우하지 않은 사람도 여권 표지·친구 목록에서 보므로 프로필 사진은 볼 수 있다
        mvc.perform(get("/api/travelers/{no}", mina.getPassportNo()).with(as(stranger)))
                .andExpect(jsonPath("$.profileImageUrl").value(url));
        mvc.perform(get(url).with(as(stranger))).andExpect(status().isOk());
        // 로그인은 해야 한다
        mvc.perform(get(url)).andExpect(status().isUnauthorized());
    }

    @Test
    void 다시_바꾸거나_원래대로_하면_전_사진은_사라진다() throws Exception {
        User jun = user(5003L, "준");
        String first = change(jun);
        String second = change(jun);
        mvc.perform(get(first).with(as(jun))).andExpect(status().isNotFound());
        mvc.perform(get(second).with(as(jun))).andExpect(status().isOk());

        mvc.perform(delete("/api/me/photo").with(as(jun)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileImageUrl").doesNotExist());
        mvc.perform(get(second).with(as(jun))).andExpect(status().isNotFound());
    }

    @Test
    void 기록_사진은_프로필_사진_주소로_볼_수_없다() throws Exception {
        User owner = user(5004L, "주인");
        User stranger = user(5005L, "낯선이2");
        String body = mvc.perform(multipart(HttpMethod.POST, "/api/visits/3203/entries").file(jpeg("trip.jpg")).with(as(owner)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String photoUrl = JsonPath.read(body, "$.photoUrl");
        String id = photoUrl.substring(photoUrl.lastIndexOf('/') + 1);
        mvc.perform(get("/api/avatars/{id}", id).with(as(stranger))).andExpect(status().isNotFound());
    }

    @Test
    void 사진_파일이_아니면_400() throws Exception {
        User u = user(5006L, "오류");
        mvc.perform(multipart(HttpMethod.PUT, "/api/me/photo")
                        .file(new org.springframework.mock.web.MockMultipartFile("photo", "a.txt", "text/plain", new byte[]{1}))
                        .with(as(u)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("JPG, PNG, WEBP, GIF, HEIC 사진만 올릴 수 있어요"));
    }
}
