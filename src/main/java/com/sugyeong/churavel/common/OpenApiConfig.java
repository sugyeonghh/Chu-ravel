package com.sugyeong.churavel.common;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * API 명세서 (springdoc). 로컬에서 http://localhost:5173/swagger-ui.html (또는 :8080) 로 본다. prod 프로필에서는 꺼 둔다.
 * 로그인은 세션 쿠키라 같은 브라우저에서 앱에 로그인해 두면 Swagger UI 의 "Try it out" 도 그 사용자로 불린다.
 * <p>
 * 응답 코드: 컨트롤러의 {@code @ApiResponse} 에는 그 API 만의 성공·오류만 적는다. 모두에게 같은 것은 {@link #commonResponses()} 가 붙인다.
 */
@Configuration
public class OpenApiConfig {

    private static final String SESSION = "session";
    private static final String PROBLEM = "ProblemDetail";
    /** 로그인 없이 쓰는 API (SecurityConfig 와 같게) */
    private static final List<String> PUBLIC = List.of("/api/regions", "/api/config", "/api/auth/");
    private static final Set<PathItem.HttpMethod> CHANGES = Set.of(
            PathItem.HttpMethod.POST, PathItem.HttpMethod.PUT, PathItem.HttpMethod.PATCH, PathItem.HttpMethod.DELETE);
    private static final String CSRF = "XSRF-TOKEN 쿠키 값을 X-XSRF-TOKEN 헤더로 보내지 않았어요 (요청이 거부됐어요)";

    @Bean
    OpenAPI churavelOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("츄~♡레블맵 API")
                        .version("v1")
                        .description("""
                                여권(지도) · 체크인과 기록 · 친구 · 댓글 API.

                                - **로그인**: 앱(같은 브라우저)에서 카카오나 아이디로 로그인해 두면 세션 쿠키(JSESSIONID)로 불린다. \
                                `/api/regions`·`/api/config`·`/api/auth/**` 말고는 로그인이 필요하다 (안 했으면 401).
                                - **CSRF**: PUT·POST·DELETE 는 `XSRF-TOKEN` 쿠키 값을 `X-XSRF-TOKEN` 헤더로 보낸다 (없으면 403). 이 화면은 자동으로 붙인다.
                                - **오류**: 모두 ProblemDetail JSON (`application/problem+json`). `detail` 은 사용자에게 그대로 보여줄 한국어 문장이다.
                                - **코드**: 한국 시·군·구 = KOSTAT 2013 앞 4자리(`3203`), 다른 나라 지역 = Natural Earth adm1_code(`JPN-1860`), 나라 = ISO alpha-2(`JP`). 사람은 여권번호(`M12345678`).
                                """))
                .components(new Components()
                        .addSecuritySchemes(SESSION, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.COOKIE).name("JSESSIONID")
                                .description("앱에 로그인하면 브라우저가 알아서 보낸다"))
                        .addSchemas(PROBLEM, problemSchema()))
                .addSecurityItem(new SecurityRequirement().addList(SESSION));
    }

    /**
     * 모든 API 에 공통 응답을 붙인다.
     * - 로그인이 필요한 API: 401 / 바꾸는 요청(PUT·POST·DELETE): 403 (CSRF). 그 API 가 이미 403 을 적었으면 설명에 덧붙인다
     * - 4xx 본문은 ProblemDetail, 204 는 본문 없음 / 공개 API 는 자물쇠(세션)를 뗀다 / 응답 코드는 작은 것부터
     */
    @Bean
    OpenApiCustomizer commonResponses() {
        return openApi -> openApi.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, op) -> {
            ApiResponses responses = op.getResponses();
            boolean open = PUBLIC.stream().anyMatch(path::startsWith);
            if (open) {
                op.setSecurity(List.of());
            } else {
                responses.putIfAbsent("401", new ApiResponse().description("로그인이 필요해요"));
            }
            if (CHANGES.contains(method)) {
                ApiResponse forbidden = responses.get("403");
                if (forbidden == null) {
                    responses.addApiResponse("403", new ApiResponse().description(CSRF));
                } else {
                    forbidden.setDescription(forbidden.getDescription() + ", 또는 " + CSRF);
                }
            }
            // springdoc 은 적어 둔 오류 응답에도 성공 반환 타입을 본문으로 달아서 갈아 끼운다. 204 는 본문이 없다
            responses.forEach((code, response) -> {
                if (code.startsWith("4")) {
                    response.setContent(new Content().addMediaType("application/problem+json",
                            new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + PROBLEM))));
                } else if (code.equals("204")) {
                    response.setContent(null);
                }
            });
            ApiResponses sorted = new ApiResponses();
            responses.entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                    .forEach(e -> sorted.addApiResponse(e.getKey(), e.getValue()));
            op.setResponses(sorted);
        }));
    }

    /** RFC 9457 ProblemDetail (ApiExceptionHandler, SecurityConfig.problem) */
    private static Schema<?> problemSchema() {
        return new ObjectSchema()
                .description("오류 응답. detail 을 토스트로 그대로 보여준다")
                .addProperty("status", new IntegerSchema().example(404))
                .addProperty("title", new StringSchema().example("Not Found"))
                .addProperty("detail", new StringSchema().example("여권번호가 M12345678인 여행자를 찾지 못했어요"))
                .addProperty("instance", new StringSchema().example("/api/travelers/M12345678"));
    }
}
