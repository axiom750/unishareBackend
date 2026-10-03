package com.unishare.architecture;

import com.unishare.UnishareApplication;
import com.unishare.entity.rbac.GodAccount;
import com.unishare.entity.rbac.GodAccountRole;
import com.unishare.entity.rbac.GodRole;
import com.unishare.entity.rbac.GodRolePermission;
import com.unishare.entity.rbac.Permission;
import com.unishare.enums.rbac.PermissionDomain;
import com.unishare.entity.rbac.Role;
import com.unishare.entity.user.User;
import com.unishare.repository.auth.GodAccountRepository;
import com.unishare.repository.auth.GodAccountRoleRepository;
import com.unishare.repository.auth.GodRolePermissionRepository;
import com.unishare.repository.auth.GodRoleRepository;
import com.unishare.utils.synchronizer.SecurityDeclarationSynchronizer;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Table;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.asm.ClassReader;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.asm.Type;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Enforces the control-plane architecture:
 *
 * 1. SecurityDeclarationSynchronizer is the ONLY production class allowed to
 *    create/update/delete GodAccount, GodRole and GodAccountRole
 *    (repository save/delete, entity setters, builders, constructors).
 * 2. GodRole is structurally separate from the normal RBAC Role.
 *
 * Works on compiled bytecode, so it also covers controllers, OAuth,
 * registration, authentication and any future class.
 */
@DisplayName("Control-plane lifecycle ownership & isolation")
class ControlPlaneLifecycleOwnershipTest {

    private static final String OWNER = Type.getInternalName(SecurityDeclarationSynchronizer.class);

    private static final Set<String> GOD_REPOSITORIES = Set.of(
            Type.getInternalName(GodAccountRepository.class),
            Type.getInternalName(GodRoleRepository.class),
            Type.getInternalName(GodAccountRoleRepository.class),
            Type.getInternalName(GodRolePermissionRepository.class)
    );

    private static final Set<String> GOD_ENTITIES = Set.of(
            Type.getInternalName(GodAccount.class),
            Type.getInternalName(GodRole.class),
            Type.getInternalName(GodAccountRole.class),
            Type.getInternalName(GodRolePermission.class)
    );

    // ---------------------------------------------------------------- lifecycle ownership

    @Test
    @DisplayName("only SecurityDeclarationSynchronizer mutates GodAccount/GodRole/GodAccountRole/GodRolePermission")
    void onlySynchronizerMutatesControlPlane() throws Exception {

        List<String> violations = new ArrayList<>();
        int scanned = 0;

        for (Path classFile : productionClassFiles()) {
            scanned++;
            try (InputStream in = Files.newInputStream(classFile)) {
                new ClassReader(in).accept(new MutationDetector(violations), ClassReader.SKIP_DEBUG);
            }
        }

        assertTrue(scanned > 50, "expected to scan the production classes, scanned " + scanned);
        assertTrue(violations.isEmpty(),
                "Unauthorized control-plane mutations (only " + OWNER + " may mutate):\n  "
                        + String.join("\n  ", violations));
    }

    @Test
    @DisplayName("the synchronizer really is the mutation site (detector sanity check)")
    void detectorSeesSynchronizerMutations() throws Exception {

        List<String> found = new ArrayList<>();
        Path synchronizer = classesRoot().resolve(OWNER + ".class");

        try (InputStream in = Files.newInputStream(synchronizer)) {
            new ClassReader(in).accept(new MutationDetector(found, true), ClassReader.SKIP_DEBUG);
        }

        assertTrue(found.stream().anyMatch(v -> v.contains("GodAccountRepository.save")), found.toString());
        assertTrue(found.stream().anyMatch(v -> v.contains("GodRoleRepository.save")), found.toString());
        assertTrue(found.stream().anyMatch(v -> v.contains("GodAccountRoleRepository.save")), found.toString());
        assertTrue(found.stream().anyMatch(v -> v.contains("GodRolePermissionRepository.save")), found.toString());
        assertTrue(found.stream().anyMatch(v -> v.contains("GodRolePermissionRepository.delete")), found.toString());
    }

    private static final class MutationDetector extends ClassVisitor {

        private final List<String> violations;
        private final boolean includeOwner;
        private String className;

        MutationDetector(List<String> violations) {
            this(violations, false);
        }

        MutationDetector(List<String> violations, boolean includeOwner) {
            super(Opcodes.ASM9);
            this.violations = violations;
            this.includeOwner = includeOwner;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            className = name;
        }

        @Override
        public MethodVisitor visitMethod(int access, String methodName, String descriptor, String signature, String[] exceptions) {

            if (!includeOwner && (className.equals(OWNER) || isGodEntityOrItsBuilder(className))) {
                return null;
            }

            return new MethodVisitor(Opcodes.ASM9) {

                @Override
                public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {

                    boolean repositoryMutation = GOD_REPOSITORIES.contains(owner)
                            && (name.startsWith("save") || name.startsWith("delete"));

                    boolean entityMutation = isGodEntityOrItsBuilder(owner)
                            && (name.startsWith("set") || name.equals("builder") || name.equals("<init>"));

                    if (repositoryMutation || entityMutation) {
                        violations.add(className.replace('/', '.') + "#" + methodName + " -> "
                                + owner.substring(owner.lastIndexOf('/') + 1) + "." + name);
                    }
                }
            };
        }

        private static boolean isGodEntityOrItsBuilder(String internalName) {
            String outer = internalName.contains("$")
                    ? internalName.substring(0, internalName.indexOf('$'))
                    : internalName;
            return GOD_ENTITIES.contains(outer);
        }
    }

    private static Path classesRoot() throws URISyntaxException {
        return Path.of(UnishareApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    }

    private static List<Path> productionClassFiles() throws URISyntaxException, IOException {
        try (Stream<Path> files = Files.walk(classesRoot())) {
            return files.filter(p -> p.toString().endsWith(".class")).toList();
        }
    }

    // ---------------------------------------------------------------- structural isolation

    @Test
    @DisplayName("GodRole is a separate entity in god_roles, not a Role")
    void godRoleIsSeparateEntity() {

        assertEquals(Object.class, GodRole.class.getSuperclass());
        assertFalse(Role.class.isAssignableFrom(GodRole.class));
        assertEquals("god_roles", GodRole.class.getAnnotation(Table.class).name());
        assertEquals("roles", Role.class.getAnnotation(Table.class).name());
    }

    @Test
    @DisplayName("GodAccountRole references GodRole (god_role_id), never Role")
    void godAccountRoleReferencesGodRole() throws NoSuchFieldException {

        Field godRole = GodAccountRole.class.getDeclaredField("godRole");
        assertEquals(GodRole.class, godRole.getType());
        assertEquals("god_role_id", godRole.getAnnotation(JoinColumn.class).name());
        assertTrue(noFieldOfType(GodAccountRole.class, Role.class), "GodAccountRole must not reference Role");
    }

    @Test
    @DisplayName("users -> user_roles -> roles only: a User cannot hold a GodRole")
    void userRolesReferenceNormalRolesOnly() throws NoSuchFieldException {

        Field roles = User.class.getDeclaredField("roles");
        ParameterizedType type = (ParameterizedType) roles.getGenericType();

        assertEquals(Role.class, type.getActualTypeArguments()[0]);
        assertEquals("user_roles", roles.getAnnotation(JoinTable.class).name());
        assertTrue(noFieldOfType(User.class, GodRole.class));
        assertTrue(noFieldOfType(User.class, GodAccount.class));
    }

    @Test
    @DisplayName("normal Role (and its permissions) never references the control plane")
    void roleKnowsNothingAboutControlPlane() {

        assertTrue(noFieldOfType(Role.class, GodRole.class));
        assertTrue(noFieldOfType(Role.class, GodAccount.class));
        assertTrue(noFieldOfType(GodRole.class, Role.class));
        assertTrue(noFieldOfType(GodRole.class, Permission.class));
    }

    @Test
    @DisplayName("god_role_permissions references GodRole and Permission, never Role")
    void godRolePermissionMapping() throws NoSuchFieldException {

        assertEquals("god_role_permissions", GodRolePermission.class.getAnnotation(Table.class).name());

        Field godRole = GodRolePermission.class.getDeclaredField("godRole");
        assertEquals(GodRole.class, godRole.getType());
        assertEquals("god_role_id", godRole.getAnnotation(JoinColumn.class).name());

        Field permission = GodRolePermission.class.getDeclaredField("permission");
        assertEquals(Permission.class, permission.getType());
        assertEquals("permission_id", permission.getAnnotation(JoinColumn.class).name());

        assertTrue(noFieldOfType(GodRolePermission.class, Role.class));
        assertEquals("role_permissions", roleField("permissions").getAnnotation(JoinTable.class).name());
    }

    @Test
    @DisplayName("D. PermissionDomain is persisted as STRING and defaults to APPLICATION")
    void permissionDomainMapping() throws Exception {

        Field domain = Permission.class.getDeclaredField("domain");
        assertEquals(PermissionDomain.class, domain.getType());
        assertEquals(EnumType.STRING, domain.getAnnotation(Enumerated.class).value());

        assertEquals(PermissionDomain.APPLICATION, Permission.builder().build().getDomain());
        assertEquals(PermissionDomain.APPLICATION,
                com.unishare.annotation.Permission.class.getMethod("domain").getDefaultValue());
    }

    private static Field roleField(String name) throws NoSuchFieldException {
        return Role.class.getDeclaredField(name);
    }

    private static boolean noFieldOfType(Class<?> holder, Class<?> forbidden) {
        return Arrays.stream(holder.getDeclaredFields()).noneMatch(field ->
                forbidden.isAssignableFrom(field.getType())
                        || (field.getGenericType() instanceof ParameterizedType pt
                        && Arrays.stream(pt.getActualTypeArguments()).anyMatch(forbidden::equals)));
    }
}
