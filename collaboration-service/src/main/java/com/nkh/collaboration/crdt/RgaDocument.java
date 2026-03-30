package com.nkh.collaboration.crdt;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class RgaDocument {

    public static final String HEAD = "HEAD";

    private final Map<String, RgaNode> nodes = new HashMap<>();

    public RgaDocument() {
        nodes.put(HEAD, new RgaNode(HEAD, null, ""));
    }

    public static RgaDocument fromText(String content) {
        RgaDocument document = new RgaDocument();
        String leftId = HEAD;
        for (int index = 0; index < content.length(); index++) {
            String id = "seed-" + index;
            document.nodes.put(id, new RgaNode(id, leftId, String.valueOf(content.charAt(index))));
            leftId = id;
        }
        return document;
    }

    public List<InsertAtom> insert(int index, String value, String actorId, long version) {
        List<String> visibleIds = visibleNodeIds();
        String leftId = index == 0 ? HEAD : visibleIds.get(index - 1);
        List<InsertAtom> atoms = new ArrayList<>();
        for (int i = 0; i < value.length(); i++) {
            String id = actorId + ":" + version + ":" + i + ":" + UUID.randomUUID();
            InsertAtom atom = new InsertAtom(id, leftId, String.valueOf(value.charAt(i)));
            applyInsert(atom);
            atoms.add(atom);
            leftId = id;
        }
        return atoms;
    }

    public List<String> delete(int index, int length) {
        List<String> visibleIds = visibleNodeIds();
        int endExclusive = Math.min(index + length, visibleIds.size());
        List<String> deletedIds = new ArrayList<>(visibleIds.subList(index, endExclusive));
        applyDelete(deletedIds);
        return deletedIds;
    }

    public void applyInsert(InsertAtom atom) {
        nodes.putIfAbsent(atom.id(), new RgaNode(atom.id(), atom.leftId(), atom.value()));
    }

    public void applyDelete(List<String> deletedIds) {
        for (String deletedId : deletedIds) {
            RgaNode node = nodes.get(deletedId);
            if (node != null) {
                node.setDeleted(true);
            }
        }
    }

    public String text() {
        return visibleNodeIds().stream()
                .map(nodes::get)
                .filter(node -> node != null && !node.isDeleted())
                .map(RgaNode::getValue)
                .collect(Collectors.joining());
    }

    public int length() {
        return visibleNodeIds().size();
    }

    private List<String> visibleNodeIds() {
        Map<String, List<RgaNode>> adjacency = new HashMap<>();
        for (RgaNode node : nodes.values()) {
            if (HEAD.equals(node.getId())) {
                continue;
            }
            adjacency.computeIfAbsent(node.getLeftId(), ignored -> new ArrayList<>()).add(node);
        }
        adjacency.values().forEach(children -> children.sort(Comparator.comparing(RgaNode::getId)));
        List<String> ordered = new ArrayList<>();
        traverse(HEAD, adjacency, ordered);
        return ordered;
    }

    private void traverse(String nodeId, Map<String, List<RgaNode>> adjacency, List<String> ordered) {
        for (RgaNode child : adjacency.getOrDefault(nodeId, List.of())) {
            ordered.add(child.getId());
            traverse(child.getId(), adjacency, ordered);
        }
    }
}
