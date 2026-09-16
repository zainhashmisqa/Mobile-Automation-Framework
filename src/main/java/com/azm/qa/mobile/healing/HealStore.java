package com.azm.qa.mobile.healing;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Append-only JSONL ledger of every heal event, across every run. Never truncated, never
 * summarized away — this file is the evidence that self-healing is doing what it claims,
 * and it's what {@code RunAnalyzer} correlates failures against.
 */
public final class HealStore {

    private final Path ledgerPath;
    private final ObjectMapper mapper = new ObjectMapper();

    public HealStore(Path ledgerPath) {
        this.ledgerPath = ledgerPath;
    }

    public synchronized void append(HealEvent event) {
        try {
            Path parent = ledgerPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            String line = mapper.writeValueAsString(event) + System.lineSeparator();
            Files.writeString(ledgerPath, line,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not append to heal ledger at " + ledgerPath, e);
        }
    }
}
