package com.unishare.utils.scanner;

import com.unishare.UnishareApplication;
import com.unishare.annotation.GodAccount;
import com.unishare.annotation.GodRole;
import com.unishare.annotation.uniShareRole;
import com.unishare.utils.seeder.SecuritySeedProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Repeatable;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The fixture classes below exist only to exercise scanner validation.
 * Production declarations live exclusively on UnishareApplication.
 */
@DisplayName("SecurityDeclarationScanner")
class SecurityDeclarationScannerTest {

    private static final String V7_A = "01a0fe4b-f2ea-7000-8000-00000000000a";
    private static final String V7_B = "01a0fe4b-f2ea-7000-8000-00000000000b";
    private static final String V7_C = "01a0fe4b-f2ea-7000-8000-00000000000c";
    private static final String V7_D = "01a0fe4b-f2ea-7000-8000-00000000000d";

    private final SecurityDeclarationScanner scanner = new SecurityDeclarationScanner();

    private static SecuritySeedProperties.Scanning allScanning() {
        return new SecuritySeedProperties.Scanning();
    }

    private IllegalStateException scanFails(Class<?> fixture) {
        return assertThrows(IllegalStateException.class, () -> scanner.scan(fixture, allScanning()));
    }

    // ---------------------------------------------------------------- A

    @Test
    @DisplayName("A. frozen identity pin: real UnishareApplication declarations (change = build failure)")
    void scansUnishareApplication() {

        SecurityDeclarations declarations = scanner.scan(allScanning());

        assertEquals(UnishareApplication.class, SecurityDeclarationScanner.DECLARATION_SOURCE);

        assertEquals(UUID.fromString("01a0fe4b-f2ea-7d8c-9a3f-1e5c4b7a9d2f"), declarations.godAccount().id());
        assertEquals("superuser", declarations.godAccount().username());
        assertEquals("superuser.unishare@gmail.com", declarations.godAccount().email());
        assertEquals("UniShare God Account", declarations.godAccount().displayName());

        assertEquals(UUID.fromString("01a0fe4b-f2ea-72bb-b39f-fe77132c0be8"), declarations.godRole().id());
        assertEquals("SUPERUSER", declarations.godRole().name());

        assertEquals(
                List.of(
                        UUID.fromString("01a0fe4b-f2ea-73a5-ad8d-866b14ba8160"),
                        UUID.fromString("01a0fe4b-f2ea-7efb-8b5b-826023276850"),
                        UUID.fromString("01a0fe4b-f2ea-7aea-94fe-7a296412f964")
                ),
                declarations.roles().stream().map(uniShareRoleDefinition::id).toList()
        );
        assertEquals(List.of("USER", "MODERATOR", "ADMIN"),
                declarations.roles().stream().map(uniShareRoleDefinition::name).toList());

        assertEquals(declarations.roles().get(0), declarations.defaultRole());
    }

    @Test
    @DisplayName("A. disabled scanning flags leave declarations empty")
    void disabledFlagsSkipScanning() {

        SecuritySeedProperties.Scanning scanning = allScanning();
        scanning.setGodAccount(false);
        scanning.setGodRole(false);
        scanning.setRoles(false);

        SecurityDeclarations declarations = scanner.scan(Valid.class, scanning);

        assertNull(declarations.godAccount());
        assertNull(declarations.godRole());
        assertTrue(declarations.roles().isEmpty());
        assertThrows(IllegalStateException.class, declarations::defaultRole);
    }

    // ---------------------------------------------------------------- B / C

    @Test
    @DisplayName("B. missing @GodAccount fails")
    void missingGodAccount() {
        assertTrue(scanFails(MissingGodAccount.class).getMessage().contains("@GodAccount"));
    }

    @Test
    @DisplayName("C. multiple @GodAccount is impossible: annotation is not @Repeatable")
    void multipleGodAccountRejectedByCompiler() {
        assertNull(GodAccount.class.getAnnotation(Repeatable.class));
    }

    // ---------------------------------------------------------------- D / E

    @Test
    @DisplayName("D. missing @GodRole fails")
    void missingGodRole() {
        assertTrue(scanFails(MissingGodRole.class).getMessage().contains("@GodRole"));
    }

    @Test
    @DisplayName("E. multiple @GodRole is impossible: annotation is not @Repeatable")
    void multipleGodRoleRejectedByCompiler() {
        assertNull(GodRole.class.getAnnotation(Repeatable.class));
    }

    // ---------------------------------------------------------------- F / G / H

    @Test
    @DisplayName("F. no @uniShareRole fails")
    void noNormalRoles() {
        assertTrue(scanFails(NoRoles.class).getMessage().contains("@uniShareRole"));
    }

    @Test
    @DisplayName("G. duplicate role UUID fails")
    void duplicateRoleId() {
        assertTrue(scanFails(DuplicateRoleId.class).getMessage().contains("Duplicate @uniShareRole id"));
    }

    @Test
    @DisplayName("H. duplicate role name fails (case-insensitive)")
    void duplicateRoleName() {
        assertTrue(scanFails(DuplicateRoleName.class).getMessage().contains("Duplicate @uniShareRole name"));
    }

    @Test
    @DisplayName("GodRole UUID colliding with a normal role fails")
    void godRoleIdCollision() {
        assertTrue(scanFails(GodRoleIdCollision.class).getMessage().contains("must not reuse the @GodRole id"));
    }

    @Test
    @DisplayName("GodRole name colliding with a normal role fails")
    void godRoleNameCollision() {
        assertTrue(scanFails(GodRoleNameCollision.class).getMessage().contains("must not use the @GodRole name"));
    }

    @Test
    @DisplayName("SUPERUSER (the GodRole name) as a normal role fails")
    void superuserAsNormalRole() {
        assertTrue(scanFails(SuperuserAsNormalRole.class).getMessage()
                .contains("must not use the @GodRole name: SUPERUSER"));
    }

    @Test
    @DisplayName("normal role reusing the GodAccount id fails")
    void godAccountIdReuse() {
        assertTrue(scanFails(GodAccountIdReuse.class).getMessage().contains("must not reuse the @GodAccount id"));
    }

    @Test
    @DisplayName("normal role using the GodAccount username fails")
    void godAccountUsernameReuse() {
        assertTrue(scanFails(GodAccountUsernameReuse.class).getMessage().contains("must not use the @GodAccount username"));
    }

    @Test
    @DisplayName("GodAccount and GodRole sharing an id fails")
    void godAccountGodRoleSameId() {
        assertTrue(scanFails(GodAccountGodRoleSameId.class).getMessage().contains("must differ"));
    }

    // ---------------------------------------------------------------- I / J

    @Test
    @DisplayName("I. malformed UUID fails")
    void invalidUuid() {
        assertTrue(scanFails(InvalidUuid.class).getMessage().contains("not a valid UUID"));
    }

    @Test
    @DisplayName("J. non-v7 UUID fails")
    void nonV7Uuid() {
        assertTrue(scanFails(NonV7Uuid.class).getMessage().contains("UUID v7"));
    }

    @Test
    @DisplayName("blank metadata fails")
    void blankMetadata() {
        assertTrue(scanFails(BlankMetadata.class).getMessage().contains("cannot be blank"));
    }

    // ================================================================ fixtures

    @GodAccount(id = V7_A, username = "god", email = "God@Example.com", displayName = "God")
    @GodRole(id = V7_B, name = "ROOT", description = "root")
    @uniShareRole(id = V7_C, name = "MEMBER", description = "member")
    static class Valid {
    }

    @GodRole(id = V7_B, name = "ROOT", description = "root")
    @uniShareRole(id = V7_C, name = "MEMBER", description = "member")
    static class MissingGodAccount {
    }

    @GodAccount(id = V7_A, username = "god", email = "god@example.com", displayName = "God")
    @uniShareRole(id = V7_C, name = "MEMBER", description = "member")
    static class MissingGodRole {
    }

    @GodAccount(id = V7_A, username = "god", email = "god@example.com", displayName = "God")
    @GodRole(id = V7_B, name = "ROOT", description = "root")
    static class NoRoles {
    }

    @GodAccount(id = V7_A, username = "god", email = "god@example.com", displayName = "God")
    @GodRole(id = V7_B, name = "ROOT", description = "root")
    @uniShareRole(id = V7_C, name = "MEMBER", description = "member")
    @uniShareRole(id = V7_C, name = "OTHER", description = "other")
    static class DuplicateRoleId {
    }

    @GodAccount(id = V7_A, username = "god", email = "god@example.com", displayName = "God")
    @GodRole(id = V7_B, name = "ROOT", description = "root")
    @uniShareRole(id = V7_C, name = "MEMBER", description = "member")
    @uniShareRole(id = V7_D, name = "member", description = "member again")
    static class DuplicateRoleName {
    }

    @GodAccount(id = V7_A, username = "god", email = "god@example.com", displayName = "God")
    @GodRole(id = V7_B, name = "ROOT", description = "root")
    @uniShareRole(id = V7_B, name = "MEMBER", description = "member")
    static class GodRoleIdCollision {
    }

    @GodAccount(id = V7_A, username = "god", email = "god@example.com", displayName = "God")
    @GodRole(id = V7_B, name = "ROOT", description = "root")
    @uniShareRole(id = V7_C, name = "Root", description = "member")
    static class GodRoleNameCollision {
    }

    @GodAccount(id = V7_A, username = "god", email = "god@example.com", displayName = "God")
    @GodRole(id = V7_B, name = "SUPERUSER", description = "root")
    @uniShareRole(id = V7_C, name = "SUPERUSER", description = "normal role pretending to be root")
    static class SuperuserAsNormalRole {
    }

    @GodAccount(id = V7_A, username = "god", email = "god@example.com", displayName = "God")
    @GodRole(id = V7_B, name = "ROOT", description = "root")
    @uniShareRole(id = V7_A, name = "MEMBER", description = "member")
    static class GodAccountIdReuse {
    }

    @GodAccount(id = V7_A, username = "god", email = "god@example.com", displayName = "God")
    @GodRole(id = V7_B, name = "ROOT", description = "root")
    @uniShareRole(id = V7_C, name = "GOD", description = "member")
    static class GodAccountUsernameReuse {
    }

    @GodAccount(id = V7_A, username = "god", email = "god@example.com", displayName = "God")
    @GodRole(id = V7_A, name = "ROOT", description = "root")
    @uniShareRole(id = V7_C, name = "MEMBER", description = "member")
    static class GodAccountGodRoleSameId {
    }

    @GodAccount(id = "not-a-uuid", username = "god", email = "god@example.com", displayName = "God")
    @GodRole(id = V7_B, name = "ROOT", description = "root")
    @uniShareRole(id = V7_C, name = "MEMBER", description = "member")
    static class InvalidUuid {
    }

    @GodAccount(id = V7_A, username = "god", email = "god@example.com", displayName = "God")
    @GodRole(id = V7_B, name = "ROOT", description = "root")
    @uniShareRole(id = "01a0fe4b-f2ea-4000-8000-00000000000c", name = "MEMBER", description = "member")
    static class NonV7Uuid {
    }

    @GodAccount(id = V7_A, username = " ", email = "god@example.com", displayName = "God")
    @GodRole(id = V7_B, name = "ROOT", description = "root")
    @uniShareRole(id = V7_C, name = "MEMBER", description = "member")
    static class BlankMetadata {
    }
}
