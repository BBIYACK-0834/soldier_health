package com.teukgeupjeonsa.backend.collector.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "meal-collector")
public class MealCollectorProperties {

    private int timeoutMillis = 10000;

    /**
     * 고정 서비스 목록.
     * 숫자(예: 3389), 별칭(ATC/STANDARD), 또는 전체 서비스 코드(DS_TB_MNDT_DATEBYMLSVC_3389)를 허용한다.
     */
    private List<String> fixedServices = new ArrayList<>(List.of(
            "1570", "5861", "1691", "3182", "8623", "7296", "1862", "2171", "7021", "9030",
            "ATC", "5397", "3296", "8902", "2621", "3389", "5021", "6176", "3007", "5322",
            "5067", "7162", "1575", "6335", "7369", "2136", "1968", "6685", "2291", "7652",
            "7461", "STANDARD"
    ));

    private String servicePrefix = "DS_TB_MNDT_DATEBYMLSVC_";

    /** 별칭 ATC의 실제 OpenAPI 서비스 코드. */
    private String atcServiceCode = "DS_TB_MNDT_DATEBYMLSVC_ATC";

    /** 별칭 STANDARD의 실제 OpenAPI 서비스 코드. */
    private String standardServiceCode = "DS_TB_MNDT_DATEBYMLSVC_STANDARD";

    /** 월간 자동 수집 cron. 기본값: 매월 1일 03:00 */
    private String autoCollectCron = "0 0 3 1 * *";

    /** 자동 수집 타임존 */
    private String autoCollectZone = "Asia/Seoul";
}
