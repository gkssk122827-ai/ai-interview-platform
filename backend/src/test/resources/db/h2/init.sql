CREATE ALIAS IF NOT EXISTS DATE_ADD AS $$
java.sql.Date dateAdd(java.sql.Date baseDate, Object intervalValue) {
    if (baseDate == null || intervalValue == null) {
        return baseDate;
    }

    java.time.LocalDate localDate = baseDate.toLocalDate();

    if (intervalValue instanceof java.time.Period period) {
        return java.sql.Date.valueOf(localDate.plus(period));
    }
    if (intervalValue instanceof java.time.Duration duration) {
        return java.sql.Date.valueOf(localDate.plusDays(duration.toDays()));
    }
    if (intervalValue instanceof Number number) {
        return java.sql.Date.valueOf(localDate.plusDays(number.longValue()));
    }

    String value = intervalValue.toString().replace("'", "").trim().toUpperCase(java.util.Locale.ROOT);
    if (value.startsWith("P")) {
        return java.sql.Date.valueOf(localDate.plus(java.time.Period.parse(value)));
    }

    String[] parts = value.split("\\s+");
    if (parts.length >= 2) {
        long amount = Long.parseLong(parts[0]);
        return switch (parts[1]) {
            case "DAY", "DAYS" -> java.sql.Date.valueOf(localDate.plusDays(amount));
            case "MONTH", "MONTHS" -> java.sql.Date.valueOf(localDate.plusMonths(amount));
            case "YEAR", "YEARS" -> java.sql.Date.valueOf(localDate.plusYears(amount));
            default -> java.sql.Date.valueOf(localDate.plusDays(amount));
        };
    }

    return java.sql.Date.valueOf(localDate.plusDays(Long.parseLong(value)));
}
$$;
