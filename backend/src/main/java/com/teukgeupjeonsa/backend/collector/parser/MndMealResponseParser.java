package com.teukgeupjeonsa.backend.collector.parser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class MndMealResponseParser {

    private static final Pattern NUMBER_PATTERN =
            Pattern.compile("-?\\d+(?:\\.\\d+)?");

    private static final Pattern KCAL_IN_TEXT_PATTERN =
            Pattern.compile(
                    "(\\d+(?:\\.\\d+)?)\\s*(?:kcal|㎉)",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern DATE_PATTERN =
            Pattern.compile(
                    "\\d{4}[-.]\\d{1,2}[-.]\\d{1,2}"
            );

    private static final List<String> DATE_KEYS =
            List.of(
                    "MLSV_YMD",
                    "DATE",
                    "mealDate",
                    "급식일자",
                    "일자",
                    "날짜",
                    "급식일"
            );

    private static final List<String> BREAKFAST_KEYS =
            List.of(
                    "BRKFST",
                    "조식",
                    "breakfast",
                    "조식메뉴"
            );

    private static final List<String> LUNCH_KEYS =
            List.of(
                    "LUNCH",
                    "중식",
                    "lunch",
                    "중식메뉴"
            );

    private static final List<String> DINNER_KEYS =
            List.of(
                    "DINNER",
                    "석식",
                    "dinner",
                    "석식메뉴"
            );

    private static final List<String> UNIT_NAME_KEYS =
            List.of(
                    "UNIT_NM",
                    "UNIT_NAME",
                    "unitName",
                    "부대명"
            );

    private static final List<String> REGION_KEYS =
            List.of(
                    "AREA_NM",
                    "AREA_NAME",
                    "region",
                    "지역"
            );

    public List<ParsedMealRow> parseRows(
            String serviceName,
            Map<String,Object> responseBody
    ){

        if(responseBody==null ||
                responseBody.isEmpty()){

            return List.of();
        }

        Object serviceRoot =
                responseBody.get(serviceName);

        if(serviceRoot==null){

            log.warn(
                    "서비스 없음 serviceName={}",
                    serviceName
            );

            return List.of();
        }

        List<Map<String,Object>> rowMaps =
                extractRowMaps(
                        serviceRoot,
                        serviceName
                );

        List<ParsedMealRow> result =
                new ArrayList<>();

        for(Map<String,Object> row : rowMaps){

            ParsedMealRow parsed =
                    parseSingleRow(
                            row,
                            serviceName
                    );

            if(parsed!=null){
                result.add(parsed);
            }
        }

        log.info(
                "식단 row 파싱 완료 service={}, count={}",
                serviceName,
                result.size()
        );

        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String,Object>> extractRowMaps(
            Object serviceRoot,
            String serviceName
    ){

        List<Map<String,Object>> result =
                new ArrayList<>();

        // CASE1
        if(serviceRoot instanceof List<?> serviceRootList){

            for(Object item : serviceRootList){

                if(!(item instanceof Map<?,?> itemMapRaw)){
                    continue;
                }

                Map<String,Object> itemMap =
                        (Map<String,Object>) itemMapRaw;

                Object rows =
                        itemMap.get("row");

                if(rows instanceof List<?> rowList){

                    for(Object row : rowList){

                        if(row instanceof Map<?,?> rowMapRaw){

                            result.add(
                                    (Map<String,Object>)
                                            rowMapRaw
                            );
                        }
                    }
                }
            }

            if(!result.isEmpty()){
                return result;
            }
        }

        // CASE2
        if(serviceRoot instanceof Map<?,?> rootMapRaw){

            Map<String,Object> rootMap =
                    (Map<String,Object>) rootMapRaw;

            Object rows =
                    rootMap.get("row");

            if(rows instanceof List<?> rowList){

                for(Object row : rowList){

                    if(row instanceof Map<?,?> rowMapRaw){

                        result.add(
                                (Map<String,Object>)
                                        rowMapRaw
                        );
                    }
                }
            }

            if(!result.isEmpty()){
                return result;
            }

            // CASE3
            for(Map.Entry<String,Object> entry :
                    rootMap.entrySet()){

                Object nested =
                        entry.getValue();

                if(!(nested instanceof Map<?,?> nestedMapRaw)){
                    continue;
                }

                Map<String,Object> nestedMap =
                        (Map<String,Object>) nestedMapRaw;

                Object nestedRows =
                        nestedMap.get("row");

                if(nestedRows instanceof List<?> rowList){

                    for(Object row : rowList){

                        if(row instanceof Map<?,?> rowMapRaw){

                            result.add(
                                    (Map<String,Object>)
                                            rowMapRaw
                            );
                        }
                    }
                }
            }
        }

        if(result.isEmpty()){

            log.warn(
                    "서비스 루트 파싱 실패 service={}",
                    serviceName
            );
        }

        return result;
    }

    private ParsedMealRow parseSingleRow(
            Map<String,Object> row,
            String serviceName
    ){

        String dateText =
                firstText(
                        row,
                        DATE_KEYS
                );

        String breakfastRaw =
                blankToNull(
                        firstText(
                                row,
                                BREAKFAST_KEYS
                        )
                );

        String lunchRaw =
                blankToNull(
                        firstText(
                                row,
                                LUNCH_KEYS
                        )
                );

        String dinnerRaw =
                blankToNull(
                        firstText(
                                row,
                                DINNER_KEYS
                        )
                );

        // RAW 대응
        if(
                breakfastRaw==null &&
                lunchRaw==null &&
                dinnerRaw==null
        ){

            StringBuilder merged =
                    new StringBuilder();

            for(Object value :
                    row.values()){

                if(value==null){
                    continue;
                }

                merged.append(
                        value.toString()
                ).append(" ");
            }

            String rawText =
                    merged.toString()
                            .trim();

            if(dateText==null){

                Matcher matcher =
                        DATE_PATTERN
                                .matcher(rawText);

                if(matcher.find()){

                    dateText =
                            matcher.group();
                }
            }

            if(!rawText.isBlank()){

                lunchRaw =
                        rawText;
            }
        }

        if(dateText==null){
            return null;
        }

        LocalDate mealDate =
                parseDate(dateText);

        if(mealDate==null){

            log.warn(
                    "날짜 파싱 실패 row={}",
                    row
            );

            return null;
        }

        Integer breakfastKcal =
                parseKcalFromMealText(
                        breakfastRaw
                );

        Integer lunchKcal =
                parseKcalFromMealText(
                        lunchRaw
                );

        Integer dinnerKcal =
                parseKcalFromMealText(
                        dinnerRaw
                );

        Integer totalKcal =
                sum(
                        breakfastKcal,
                        lunchKcal,
                        dinnerKcal
                );

        return new ParsedMealRow(
                serviceName,
                mealDate,
                breakfastRaw,
                lunchRaw,
                dinnerRaw,
                breakfastKcal,
                lunchKcal,
                dinnerKcal,
                totalKcal,
                blankToNull(
                        firstText(
                                row,
                                UNIT_NAME_KEYS
                        )
                ),
                blankToNull(
                        firstText(
                                row,
                                REGION_KEYS
                        )
                )
        );
    }

    private LocalDate parseDate(
            String raw
    ){

        if(raw==null ||
                raw.isBlank()){

            return null;
        }

        String cleaned =
                raw.trim()
                        .replaceAll(
                                "\\([^)]*\\)",
                                ""
                        )
                        .replaceAll(
                                "\\s+",
                                ""
                        );

        String compact =
                cleaned.replaceAll(
                        "[^0-9]",
                        ""
                );

        try{

            if(compact.matches("\\d{8}")){

                return LocalDate.parse(
                        compact,
                        DateTimeFormatter.BASIC_ISO_DATE
                );
            }

        }catch(DateTimeParseException ignored){}

        try{

            return LocalDate.parse(
                    cleaned
                            .replace('.', '-')
                            .replace('/', '-'),
                    DateTimeFormatter.ofPattern(
                            "yyyy-M-d"
                    )
            );

        }catch(DateTimeParseException ignored){}

        return null;
    }

    private Integer parseKcalFromMealText(
            String mealText
    ){

        if(mealText==null){
            return null;
        }

        Matcher matcher =
                KCAL_IN_TEXT_PATTERN.matcher(
                        mealText
                );

        Integer max=null;

        while(matcher.find()){

            try{

                int value =
                        (int)Math.round(
                                Double.parseDouble(
                                        matcher.group(1)
                                )
                        );

                if(max==null ||
                        value>max){

                    max=value;
                }

            }catch(Exception ignored){}
        }

        return max;
    }

    private Integer sum(Integer... values){

        int sum=0;

        for(Integer value : values){

            if(value!=null){
                sum+=value;
            }
        }

        return sum==0 ?
                null :
                sum;
    }

    private String firstText(
            Map<String,Object> row,
            List<String> aliases
    ){

        for(String alias : aliases){

            for(Map.Entry<String,Object> e :
                    row.entrySet()){

                if(
                        normalize(
                                e.getKey()
                        )
                                .equals(
                                        normalize(alias)
                                )
                ){

                    Object value =
                            e.getValue();

                    if(value!=null){

                        String text =
                                value.toString()
                                        .trim();

                        if(!text.isBlank()){

                            return text;
                        }
                    }
                }
            }
        }

        return null;
    }

    private String normalize(
            String input
    ){

        return input==null ?
                "" :
                input.toLowerCase(Locale.ROOT)
                        .replaceAll(
                                "[^a-z0-9가-힣]",
                                ""
                        );
    }

    private String blankToNull(
            String text
    ){

        return text==null ||
                text.isBlank()
                ? null
                : text;
    }

    public record ParsedMealRow(
            String serviceName,
            LocalDate mealDate,
            String breakfastRaw,
            String lunchRaw,
            String dinnerRaw,
            Integer breakfastKcal,
            Integer lunchKcal,
            Integer dinnerKcal,
            Integer totalKcal,
            String unitName,
            String regionName
    ){}
}