package com.xqy.nexusedu.education;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class TeacherUserIdSerializationTest {
    private static final Path MAPPER = Path.of(
            "src/main/resources/mapper/education/infrastructure/persistence/StudentSchoolMapper.xml");

    @Test
    void serializesLargeTeacherUserIdsAsStrings() throws Exception {
        String xml = Files.readString(MAPPER, StandardCharsets.UTF_8);
        for (String statement : new String[] { "findTeachers", "findTeacherCandidates", "findTeacher",
                "findClassTeacherAssignments", "findClassTeacherAssignment" }) {
            String select = selectBody(xml, statement);
            assertTrue(select.contains("CAST(") && select.matches("(?s).*CAST\\([^)]*(?:user_id|u\\.id)[^)]* AS CHAR\\).*"),
                    statement + " must serialize user_id as CHAR");
        }
    }

    private static String selectBody(String xml, String id) {
        String marker = "<select id=\"" + id + "\"";
        int start = xml.indexOf(marker);
        int end = xml.indexOf("</select>", start);
        if (start < 0 || end < 0) throw new AssertionError("Missing mapper select: " + id);
        return xml.substring(start, end);
    }
}
