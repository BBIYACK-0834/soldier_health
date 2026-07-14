package com.teukgeupjeonsa.backend.nutrition;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class MealMenuItemParser {

    private static final Pattern ALLERGY_CODE = Pattern.compile("\\(\\s*\\d{1,2}\\s*\\)");
    private static final Pattern UNIT_ONLY = Pattern.compile("(?i)^(?:\\d+(?:\\.\\d+)?\\s*)?(?:ml|g|kg|캔|팩|병|개|봉)(?:\\s*/\\s*(?:캔|팩|병|개|봉))?$|^\\d+(?:\\.\\d+)?$");
    private static final Pattern TRAILING_FRAGMENT = Pattern.compile("^[^가-힣A-Za-z0-9]*[가-힣A-Za-z0-9\\s]*(?:\\)+)$");
    private static final List<String> ATTACH_WORDS = List.of("연간", "부대계약", "부대 계약");
    private static final List<String> SOFT_DRINK_PRODUCTS = List.of("코카콜라", "콜라", "칠성사이다", "사이다", "환타", "펩시");
    private static final List<String> BRAND_ONLY = List.of("농심", "롯데", "동원", "빙그레", "오뚜기", "서울우유", "매일유업", "남양유업");

    public List<String> parse(String rawMenu) {
        if (rawMenu == null || rawMenu.isBlank()) {
            return List.of();
        }

        List<String> items = new ArrayList<>();
        for (String commaPart : rawMenu.replace("\r\n", "\n").replace('\r', '\n').split(",")) {
            parseCommaPart(commaPart, items);
        }
        return items.stream()
                .map(this::normalizeFinalItem)
                .filter(item -> !item.isBlank())
                .filter(item -> !isStandaloneUnit(item))
                .filter(item -> !isGenericSoftDrink(item))
                .distinct()
                .toList();
    }

    private void parseCommaPart(String text, List<String> items) {
        String current = null;
        for (String rawLine : text.split("\n")) {
            String line = cleanLine(rawLine);
            if (line.isBlank()) {
                continue;
            }

            if (current == null) {
                current = line;
                continue;
            }

            if (shouldAttach(current, line)) {
                current = current + " " + line;
                continue;
            }

            addItem(items, current);
            current = line;
        }
        addItem(items, current);
    }

    private boolean shouldAttach(String current, String line) {
        if (hasOpenParenthesis(current)) {
            return true;
        }
        if (isStandaloneUnit(line) || isAttachWord(line) || isClosingFragment(line)) {
            return true;
        }
        if (isGenericSoftDrink(current) && isSoftDrinkProduct(line)) {
            return true;
        }
        if (isBrandOnly(current) && !isStandaloneUnit(line)) {
            return true;
        }
        return false;
    }

    private void addItem(List<String> items, String rawItem) {
        String item = normalizeFinalItem(rawItem);
        if (item.isBlank() || isStandaloneUnit(item)) {
            return;
        }

        if (!items.isEmpty() && isGenericSoftDrink(items.get(items.size() - 1)) && isSoftDrinkProduct(item)) {
            items.set(items.size() - 1, item);
            return;
        }
        if (isGenericSoftDrink(item)) {
            items.add(item);
            return;
        }
        items.add(item);
    }

    private String cleanLine(String line) {
        return ALLERGY_CODE.matcher(line == null ? "" : line).replaceAll(" ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String normalizeFinalItem(String rawItem) {
        String item = cleanLine(rawItem);
        if (item.isBlank()) {
            return "";
        }

        item = item.replace('（', '(').replace('）', ')');
        item = item.replaceAll("[()]+", " ");
        for (String word : List.of("부대계약", "부대 계약", "계약", "연간", "후식", "제공")) {
            item = item.replace(word, " ");
        }
        item = item.replaceAll("\\s+", " ").trim();

        if (item.startsWith("우유 ") && item.contains("백색우유")) {
            item = item.substring(item.indexOf("백색우유"));
        }
        if (item.startsWith("청량음료 ") || item.startsWith("탄산음료 ")) {
            String withoutGeneric = item.replaceFirst("^(청량음료|탄산음료)\\s+", "");
            if (isSoftDrinkProduct(withoutGeneric)) {
                item = withoutGeneric;
            }
        }
        return item.replaceAll("\\s+", " ").trim();
    }

    private boolean hasOpenParenthesis(String text) {
        int balance = 0;
        for (char ch : text.toCharArray()) {
            if (ch == '(') balance++;
            if (ch == ')') balance--;
        }
        return balance > 0;
    }

    private boolean isStandaloneUnit(String value) {
        String normalized = value == null ? "" : value.trim();
        return UNIT_ONLY.matcher(normalized).matches();
    }

    private boolean isAttachWord(String value) {
        String normalized = value == null ? "" : value.trim();
        return ATTACH_WORDS.stream().anyMatch(normalized::equals);
    }

    private boolean isClosingFragment(String value) {
        String normalized = value == null ? "" : value.trim();
        return normalized.contains(")") && TRAILING_FRAGMENT.matcher(normalized).matches();
    }

    private boolean isGenericSoftDrink(String value) {
        String normalized = compact(value);
        return "청량음료".equals(normalized) || "탄산음료".equals(normalized);
    }

    private boolean isSoftDrinkProduct(String value) {
        String normalized = compact(value);
        return SOFT_DRINK_PRODUCTS.stream().anyMatch(normalized::contains) || normalized.toLowerCase(Locale.ROOT).contains("cola");
    }

    private boolean isBrandOnly(String value) {
        String normalized = compact(value);
        return BRAND_ONLY.stream().anyMatch(normalized::equals);
    }

    private String compact(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "");
    }
}
