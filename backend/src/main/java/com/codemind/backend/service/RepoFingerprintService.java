package com.codemind.backend.service;

import com.codemind.backend.dto.RepoFingerprint;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
public class RepoFingerprintService {

    public RepoFingerprint fingerprint(String localPath) throws Exception {
        Path repoRoot = Paths.get(localPath);
        File pomFile = repoRoot.resolve("pom.xml").toFile();
        File gradleFile = repoRoot.resolve("build.gradle").toFile();

        String projectType;
        String groupId = null;
        String artifactId = null;

        if (pomFile.exists()) {
            projectType = "maven";
            String[] coords = readMavenCoordinates(pomFile);
            groupId = coords[0];
            artifactId = coords[1];
        } else if (gradleFile.exists()) {
            projectType = "gradle";
        } else {
            projectType = "unknown";
        }

        List<String> javaFiles = listJavaFiles(repoRoot);

        return new RepoFingerprint(projectType, groupId, artifactId, javaFiles.size(), javaFiles);
    }

    private String[] readMavenCoordinates(File pomFile) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(pomFile);
        doc.getDocumentElement().normalize();

        Element root = doc.getDocumentElement(); // <project>
        String groupId = getDirectChildText(root, "groupId");
        String artifactId = getDirectChildText(root, "artifactId");

        // If groupId isn't set directly on <project>, it's often inherited from <parent>
        if (groupId == null) {
            NodeList parents = root.getElementsByTagName("parent");
            if (parents.getLength() > 0) {
                groupId = getDirectChildText((Element) parents.item(0), "groupId");
            }
        }

        return new String[]{groupId, artifactId};
    }

    private String getDirectChildText(Element parent, String tagName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeName().equals(tagName)) {
                return children.item(i).getTextContent().trim();
            }
        }
        return null;
    }

    private List<String> listJavaFiles(Path repoRoot) throws Exception {
        List<String> result = new ArrayList<>();
        if (!Files.exists(repoRoot)) {
            return result;
        }
        try (Stream<Path> paths = Files.walk(repoRoot)) {
            paths.filter(p -> p.toString().endsWith(".java"))
                    .forEach(p -> result.add(repoRoot.relativize(p).toString()));
        }
        return result;
    }
}
