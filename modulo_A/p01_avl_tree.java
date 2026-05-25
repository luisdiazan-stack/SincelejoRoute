// P01 – AVL Tree
// SincelejoRoute – Estructura de Datos II
// UAJS Facultad de Ingeniería
// Autor: [Tu nombre]

import java.io.*;
import java.util.*;

// ──────────────────────────────────────────────
// CLASE NODE – representa un nodo del grafo
// ──────────────────────────────────────────────
class Node {
    String id;
    String nombre;
    String tipo;
    double lat;
    double lon;
    boolean isSetp;

    public Node(String id, String nombre, String tipo,
                double lat, double lon, boolean isSetp) {
        this.id     = id;
        this.nombre = nombre;
        this.tipo   = tipo;
        this.lat    = lat;
        this.lon    = lon;
        this.isSetp = isSetp;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (%s) setp=%b", id, nombre, tipo, isSetp);
    }
}

// ──────────────────────────────────────────────
// NODO INTERNO DEL AVL
// ──────────────────────────────────────────────
class AVLNode {
    String key;       // nombre del nodo (clave de búsqueda)
    Node   value;     // objeto Node completo
    int    height;
    AVLNode left, right;

    public AVLNode(String key, Node value) {
        this.key    = key;
        this.value  = value;
        this.height = 1;
    }
}

// ──────────────────────────────────────────────
// ÁRBOL AVL
// ──────────────────────────────────────────────
class AVLTree {

    private AVLNode root;

    // ── Altura y balance ──────────────────────
    private int height(AVLNode n) {
        return n == null ? 0 : n.height;
    }

    private int balanceFactor(AVLNode n) {
        return n == null ? 0 : height(n.left) - height(n.right);
    }

    private void updateHeight(AVLNode n) {
        n.height = 1 + Math.max(height(n.left), height(n.right));
    }

    // ── Rotaciones ────────────────────────────
    private AVLNode rotateRight(AVLNode y) {
        AVLNode x  = y.left;
        AVLNode T2 = x.right;
        x.right = y;
        y.left  = T2;
        updateHeight(y);
        updateHeight(x);
        return x;
    }

    private AVLNode rotateLeft(AVLNode x) {
        AVLNode y  = x.right;
        AVLNode T2 = y.left;
        y.left  = x;
        x.right = T2;
        updateHeight(x);
        updateHeight(y);
        return y;
    }

    // ── Balance ───────────────────────────────
    private AVLNode balance(AVLNode n) {
        updateHeight(n);
        int bf = balanceFactor(n);

        // LL
        if (bf > 1 && balanceFactor(n.left) >= 0)
            return rotateRight(n);
        // LR
        if (bf > 1 && balanceFactor(n.left) < 0) {
            n.left = rotateLeft(n.left);
            return rotateRight(n);
        }
        // RR
        if (bf < -1 && balanceFactor(n.right) <= 0)
            return rotateLeft(n);
        // RL
        if (bf < -1 && balanceFactor(n.right) > 0) {
            n.right = rotateRight(n.right);
            return rotateLeft(n);
        }
        return n;
    }

    // ── INSERT ────────────────────────────────
    public void insert(String key, Node value) {
        root = insert(root, key, value);
    }

    private AVLNode insert(AVLNode n, String key, Node value) {
        if (n == null) return new AVLNode(key, value);
        int cmp = key.compareTo(n.key);
        if      (cmp < 0) n.left  = insert(n.left,  key, value);
        else if (cmp > 0) n.right = insert(n.right, key, value);
        else { n.value = value; return n; } // clave duplicada: actualiza
        return balance(n);
    }

    // ── SEARCH ────────────────────────────────
    public Node search(String name) {
        AVLNode result = search(root, name);
        return result == null ? null : result.value;
    }

    private AVLNode search(AVLNode n, String key) {
        if (n == null) return null;
        int cmp = key.compareTo(n.key);
        if      (cmp < 0) return search(n.left,  key);
        else if (cmp > 0) return search(n.right, key);
        else              return n;
    }

    // ── FILTER BY TYPE ────────────────────────
    public List<Node> filterByType(String tipo) {
        List<Node> result = new ArrayList<>();
        filterByType(root, tipo, result);
        return result;
    }

    private void filterByType(AVLNode n, String tipo, List<Node> result) {
        if (n == null) return;
        filterByType(n.left,  tipo, result);
        if (n.value.tipo.equalsIgnoreCase(tipo))
            result.add(n.value);
        filterByType(n.right, tipo, result);
    }

    // ── IN ORDER LIST ─────────────────────────
    public List<Node> inOrderList() {
        List<Node> result = new ArrayList<>();
        inOrder(root, result);
        return result;
    }

    private void inOrder(AVLNode n, List<Node> result) {
        if (n == null) return;
        inOrder(n.left, result);
        result.add(n.value);
        inOrder(n.right, result);
    }

    // ── PRETTY PRINT ──────────────────────────
    public void prettyPrint() {
        System.out.println("\n── Árbol AVL ──");
        prettyPrint(root, "", true);
    }

    private void prettyPrint(AVLNode n, String prefix, boolean isLeft) {
        if (n == null) return;
        System.out.println(prefix
            + (isLeft ? "├── " : "└── ")
            + n.key
            + " [h=" + n.height
            + " bf=" + balanceFactor(n) + "]");
        prettyPrint(n.left,  prefix + (isLeft ? "│   " : "    "), true);
        prettyPrint(n.right, prefix + (isLeft ? "│   " : "    "), false);
    }

    // ── VERIFICAR BALANCE ─────────────────────
    public boolean isBalanced() {
        return isBalanced(root);
    }

    private boolean isBalanced(AVLNode n) {
        if (n == null) return true;
        int bf = Math.abs(balanceFactor(n));
        return bf <= 1 && isBalanced(n.left) && isBalanced(n.right);
    }
}

// ──────────────────────────────────────────────
// CARGA DEL CSV
// ──────────────────────────────────────────────
class GraphLoader {

    public static List<Node> loadNodes(String csvPath) throws IOException {
        List<Node> nodes = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader(csvPath));
        String line;
        boolean inNodes = false;

        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.equals("NODES"))  { inNodes = true;  continue; }
            if (line.equals("EDGES"))  { inNodes = false; continue; }
            if (line.isEmpty() || line.startsWith("id")) continue;

            if (inNodes) {
                String[] p = line.split(",");
                if (p.length < 6) continue;
                nodes.add(new Node(
                    p[0].trim(), p[1].trim(), p[2].trim(),
                    Double.parseDouble(p[3].trim()),
                    Double.parseDouble(p[4].trim()),
                    Boolean.parseBoolean(p[5].trim())
                ));
            }
        }
        br.close();
        return nodes;
    }
}

// ──────────────────────────────────────────────
// MAIN – DEMO Y TESTS
// ──────────────────────────────────────────────
public class p01_avl_tree {

    static int passed = 0, failed = 0;

    static void check(String name, boolean cond, String detail) {
        if (cond) {
            System.out.println("✓ PASS  " + name);
            passed++;
        } else {
            System.out.println("✗ FAIL  " + name + " → " + detail);
            failed++;
        }
    }

    public static void main(String[] args) throws IOException {
        String csv = args.length > 0 ? args[0] : "../data/sincelejo_v2.csv";

        // Cargar nodos desde CSV
        List<Node> nodes = GraphLoader.loadNodes(csv);

        // Insertar en el AVL (clave = nombre)
        AVLTree avl = new AVLTree();
        for (Node n : nodes) {
            avl.insert(n.nombre, n);
        }

        // ── Pretty print ──────────────────────
        avl.prettyPrint();

        // ── TEST 1: 20 nodos insertados ───────
        System.out.println("\n── Tests ──");
        List<Node> inorder = avl.inOrderList();
        check("20 nodos en inOrder",
              inorder.size() == 20,
              "tiene " + inorder.size());

        // ── TEST 2: Factor de balance ≤ 1 ─────
        check("Árbol balanceado (|bf| ≤ 1 en todos)",
              avl.isBalanced(), "árbol desbalanceado");

        // ── TEST 3: filterByType setp → 6 ────
        List<Node> setpNodes = avl.filterByType("setp");
        check("filterByType('setp') retorna 6 nodos",
              setpNodes.size() == 6,
              "retornó " + setpNodes.size());

        // ── TEST 4: search existe ─────────────
        Node found = avl.search("UNISUCRE");
        check("search('UNISUCRE') encuentra el nodo",
              found != null && found.id.equals("N02"),
              found == null ? "null" : found.id);

        // ── TEST 5: search no existe ──────────
        Node notFound = avl.search("Nodo Inexistente");
        check("search('Nodo Inexistente') retorna null",
              notFound == null, "retornó " + notFound);

        // ── Mostrar nodos SETP ────────────────
        System.out.println("\nNodos SETP encontrados:");
        for (Node n : setpNodes)
            System.out.println("  " + n);

        // ── Resumen ───────────────────────────
        System.out.println("\n" + "=".repeat(42));
        System.out.println("  Resultados: " + passed + " passed  |  " + failed + " failed");
        System.out.println("=".repeat(42));
    }
}
