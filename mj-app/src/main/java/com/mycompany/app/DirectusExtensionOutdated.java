package com.mycompany.app;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DirectusExtensionOutdated {
    final static String DIRECTUS_HOME = "/Users/homerh/Code/directus15";
    final static ObjectMapper objectMapper = new ObjectMapper();
    final static List<String> fields = List.of(
            "dependencies",
            "devDependencies",
            "optionalDependencies",
            "peerDependencies");

    static Map<String, Dependency> loadDependencies(Path path) throws IOException {
        Map<String, Dependency> result = new HashMap<>();
        JsonNode json = objectMapper.readTree(path.toFile());
        for (String field : fields) {
            JsonNode fieldNode = json.get(field);
            if (fieldNode == null || fieldNode.isNull())
                continue;
            fieldNode.fields().forEachRemaining(entry -> {
                String depName = entry.getKey();
                String depVer = entry.getValue().asText();
                String depCat = field;
                result.put(depName, new Dependency(depName, depVer, depCat));
            });
        }
        return result;
    }

    static List<Path> findExtensionFiles() {
        List<Path> result = new ArrayList<>();
        File[] exts = Paths.get(DIRECTUS_HOME, "extensions").toFile()
                .listFiles(f -> f.isDirectory());
        for (File ext : exts) {
            result.add(ext.toPath().resolve("package.json"));
        }
        return result;
    }

    public static void main(String[] args) throws IOException {
        var canonicalDeps = loadDependencies(Paths.get(DIRECTUS_HOME, "api", "package.json"));
        var exts = findExtensionFiles();
        for (Path ext : exts) {
            System.out.println(" * " + ext.getName(ext.getNameCount() - 2));
            var deps = loadDependencies(ext);
            compare(deps, canonicalDeps);
        }
    }

    static void compare(Map<String, Dependency> deps, Map<String, Dependency> canonical) {
        deps.forEach((name, dep) -> {
            if (dep.version().equals("workspace:*"))
                return;

            var canonicalDep = canonical.get(name);
            if (canonicalDep != null) {
                if (canonicalDep.version().equals(dep.version()))
                    System.out.println("   - (match) " + dep);
                else
                    System.out.println("   - (outdated) " + dep);
            } else {
                System.out.println("   - (dangling) " + dep);
            }
        });
    }
}

record Dependency(String name, String version, String category) {
    @Override
    public final String toString() {
        return name + ":" + version + ":" + category;
    }
}
