package com.example.seqdiag.service;

import java.util.List;
import java.util.Set;

public class PlantUmlGenerator {

    public static String generate(Set<String> participants, List<SequenceExtractor.Call> calls) {
        StringBuilder sb = new StringBuilder();
        sb.append("@startuml\n");
        for (String p : participants) {
            sb.append("participant ").append(sanitize(p)).append("\n");
        }
        sb.append("\n");
        for (SequenceExtractor.Call c : calls) {
            sb.append(sanitize(c.caller)).append(" -> ").append(sanitize(c.callee)).append(": ").append(c.method).append("\n");
        }
        sb.append("@enduml\n");
        return sb.toString();
    }

    private static String sanitize(String s) {
        return s.replaceAll("\\W+", "_");
    }
}