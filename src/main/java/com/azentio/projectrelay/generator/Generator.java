package com.azentio.projectrelay.generator;

import java.io.BufferedWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public final class Generator {
    private Generator() { }

    public record Row(String id, String from, String to, BigDecimal amount, OffsetDateTime at) { }

    public static void main(String[] args) throws Exception {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 42L;
        Path output = Path.of(args.length > 1 ? args[1] : "seeded-transfers.jsonl");
        Path labels = Path.of(args.length > 2 ? args[2] : "labels.json");
        Random random = new Random(seed);
        OffsetDateTime base = OffsetDateTime.parse("2026-01-01T09:00:00Z");
        List<Row> rows = new ArrayList<>();
        List<List<String>> planted = new ArrayList<>();

        // Ten planted chains, each containing 3-6 transfers and no repeated account.
        for (int chain = 0; chain < 10; chain++) {
            int hops = 3 + random.nextInt(4);
            long accountBase = 1_000_000_000L + chain * 100L;
            BigDecimal amount = new BigDecimal("200000.00");
            OffsetDateTime at = base.plusHours(chain * 3L);
            List<String> ids = new ArrayList<>();
            for (int hop = 0; hop < hops; hop++) {
                String id = "PLANTED-" + chain + "-" + hop;
                BigDecimal next = amount.multiply(new BigDecimal("0.97"))
                        .setScale(2, RoundingMode.HALF_UP);
                rows.add(new Row(id, account(accountBase + hop),
                        account(accountBase + hop + 1), amount, at));
                ids.add(id);
                amount = next;
                at = at.plusMinutes(5 + random.nextInt(10));
            }
            planted.add(ids);
        }

        // Decoy 1: exactly one failed condition, a 45-minute hop gap.
        addDecoy(rows, "DECOY-GAP", 2_000_000_000L, base.plusHours(40),
                new BigDecimal("200000.00"), new BigDecimal("0.97"), 3, 45);

        // Decoy 2: exactly one failed condition, one hop forwards only 80%.
        addSplitDecoy(rows, base.plusHours(44));

        // Decoy 3: a valid pass-through with only two hops (below min-hops).
        addDecoy(rows, "DECOY-TWO-HOP", 2_200_000_000L, base.plusHours(48),
                new BigDecimal("200000.00"), new BigDecimal("0.97"), 2, 5);

        // Busy hub: 250 inbound edges in a single window. Amounts are below min-amount.
        long hub = 2_300_000_000L;
        OffsetDateTime hubStart = base.plusHours(50);
        for (int i = 0; i < 250; i++) {
            rows.add(new Row("HUB-IN-" + i, account(hub + i + 1), account(hub),
                    new BigDecimal("1000.00"), hubStart.plusMinutes(i % 60)));
        }
        rows.add(new Row("HUB-OUT", account(hub), account(hub + 300),
                new BigDecimal("970.00"), hubStart.plusMinutes(65)));

        // Background transfers use disjoint accounts and amounts below min-amount.
        for (int i = 0; i < 5_000; i++) {
            long account = 3_000_000_000L + i * 2L;
            BigDecimal amount = BigDecimal.valueOf(1_000 + random.nextInt(49_000))
                    .setScale(2, RoundingMode.UNNECESSARY);
            rows.add(new Row("BG-" + i, account(account), account(account + 1),
                    amount, base.plusMinutes(random.nextInt(10_000))));
        }

        rows.sort(Comparator.comparing(Row::at).thenComparing(Row::id));
        try (BufferedWriter writer = Files.newBufferedWriter(output)) {
            for (Row row : rows) {
                writer.write("{\"transferId\":\"" + row.id()
                        + "\",\"fromAccount\":\"" + row.from()
                        + "\",\"toAccount\":\"" + row.to()
                        + "\",\"amount\":" + row.amount().toPlainString()
                        + ",\"occurredAt\":\"" + row.at() + "\"}");
                writer.newLine();
            }
        }

        StringBuilder json = new StringBuilder("{\"planted\":[");
        for (int i = 0; i < planted.size(); i++) {
            if (i > 0) json.append(',');
            json.append('[');
            for (int j = 0; j < planted.get(i).size(); j++) {
                if (j > 0) json.append(',');
                json.append('\"').append(planted.get(i).get(j)).append('\"');
            }
            json.append(']');
        }
        json.append("]}");
        Files.writeString(labels, json);
        System.out.printf("Wrote %d transfers, %d planted chains, 3 decoys and a 250-edge hub%n",
                rows.size(), planted.size());
    }

    private static void addDecoy(List<Row> rows, String prefix, long accountBase,
                                 OffsetDateTime start, BigDecimal firstAmount,
                                 BigDecimal retention, int hops, int gapMinutes) {
        BigDecimal amount = firstAmount;
        OffsetDateTime at = start;
        for (int i = 0; i < hops; i++) {
            String id = prefix + "-" + i;
            rows.add(new Row(id, account(accountBase + i), account(accountBase + i + 1),
                    amount, at));
            amount = amount.multiply(retention).setScale(2, RoundingMode.HALF_UP);
            at = at.plusMinutes(gapMinutes);
        }
    }

    private static void addSplitDecoy(List<Row> rows, OffsetDateTime start) {
        long accountBase = 2_100_000_000L;
        BigDecimal first = new BigDecimal("200000.00");
        BigDecimal second = first.multiply(new BigDecimal("0.80")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal third = second.multiply(new BigDecimal("0.97")).setScale(2, RoundingMode.HALF_UP);
        rows.add(new Row("DECOY-SPLIT-0", account(accountBase), account(accountBase + 1),
                first, start));
        rows.add(new Row("DECOY-SPLIT-1", account(accountBase + 1), account(accountBase + 2),
                second, start.plusMinutes(5)));
        rows.add(new Row("DECOY-SPLIT-2", account(accountBase + 2), account(accountBase + 3),
                third, start.plusMinutes(10)));
    }

    private static String account(long value) {
        return String.format("%010d", value);
    }
}
