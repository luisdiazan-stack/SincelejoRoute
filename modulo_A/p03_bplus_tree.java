// P03 – B+ Tree orden 4
// SincelejoRoute – Estructura de Datos II
// UAJS Facultad de Ingeniería
// Autor: [Tu nombre]

import java.io.*;
import java.util.*;

// ──────────────────────────────────────────────
// CLASE ROUTE
// ──────────────────────────────────────────────
class Route {
    String origen;
    String destino;
    String[] path;
    int    costo;
    boolean esSetp;

    public Route(String origen, String destino, int costo, boolean esSetp) {
        this.origen  = origen;
        this.destino = destino;
        this.path    = new String[]{origen, destino};
        this.costo   = costo;
        this.esSetp  = esSetp;
    }

    @Override
    public String toString() {
        return String.format("%s→%s (costo=%d, setp=%b)",
                             origen, destino, costo, esSetp);
    }
}

// ──────────────────────────────────────────────
// NODO DEL B+ TREE
// ──────────────────────────────────────────────
class BPlusNode {
    static final int ORDER = 4;           // máximo ORDER-1 = 3 claves
    int[]        keys     = new int[ORDER];
    Object[]     children = new Object[ORDER + 1]; // hijos o listas de rutas
    int          keyCount = 0;
    boolean      isLeaf;
    BPlusNode    next;                    // enlace entre hojas

    public BPlusNode(boolean isLeaf) {
        this.isLeaf = isLeaf;
    }
}

// ──────────────────────────────────────────────
// B+ TREE
// ──────────────────────────────────────────────
class BPlusTree {

    private BPlusNode root;
    private BPlusNode firstLeaf;

    public BPlusTree() {
        root      = new BPlusNode(true);
        firstLeaf = root;
    }

    // ── INSERT ────────────────────────────────

    public void insert(int key, Route route) {
       
        Object[] result = insertRecursive(root, key, route);
        if (result != null) {
            // La raíz se dividió — crear nueva raíz
            BPlusNode newRoot = new BPlusNode(false);
            newRoot.keys[0]     = (int) result[0];
            newRoot.children[0] = root;
            newRoot.children[1] = result[1];
            newRoot.keyCount    = 1;
            root = newRoot;
        }
    }

    @SuppressWarnings("unchecked")
    private Object[] insertRecursive(BPlusNode node, int key, Route route) {
        if (node.isLeaf) {
            // Buscar posición
            int i = 0;
            while (i < node.keyCount && key > node.keys[i]) i++;

            // Clave duplicada: agregar ruta a la lista existente
            if (i < node.keyCount && node.keys[i] == key) {
                ((List<Route>) node.children[i]).add(route);
                return null;
            }

            // Insertar nueva clave
            for (int j = node.keyCount; j > i; j--) {
                node.keys[j]     = node.keys[j - 1];
                node.children[j] = node.children[j - 1];
            }
            node.keys[i]     = key;
            node.children[i] = new ArrayList<>(Collections.singletonList(route));
            node.keyCount++;

            // Split si necesario
            if (node.keyCount == BPlusNode.ORDER) {
                return splitLeaf(node);
            }
            return null;

        } else {
            // Nodo interno: bajar al hijo correcto
            int i = 0;
            while (i < node.keyCount && key >= node.keys[i]) i++;

            Object[] result = insertRecursive((BPlusNode) node.children[i], key, route);
            if (result == null) return null;

            // Promover clave del split
            int    promKey  = (int) result[0];
            BPlusNode right = (BPlusNode) result[1];

            for (int j = node.keyCount; j > i; j--) {
                node.keys[j]         = node.keys[j - 1];
                node.children[j + 1] = node.children[j];
            }
            node.keys[i]         = promKey;
            node.children[i + 1] = right;
            node.keyCount++;

            if (node.keyCount == BPlusNode.ORDER) {
                return splitInternal(node);
            }
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Object[] splitLeaf(BPlusNode leaf) {
        int mid      = BPlusNode.ORDER / 2;
        BPlusNode newLeaf = new BPlusNode(true);

        newLeaf.keyCount = leaf.keyCount - mid;
        for (int i = 0; i < newLeaf.keyCount; i++) {
            newLeaf.keys[i]     = leaf.keys[mid + i];
            newLeaf.children[i] = leaf.children[mid + i];
        }
        leaf.keyCount = mid;

        // Enlazar hojas
        newLeaf.next = leaf.next;
        leaf.next    = newLeaf;

        return new Object[]{newLeaf.keys[0], newLeaf};
    }

    private Object[] splitInternal(BPlusNode node) {
        int mid         = BPlusNode.ORDER / 2;
        int promKey     = node.keys[mid];
        BPlusNode right = new BPlusNode(false);

        right.keyCount = node.keyCount - mid - 1;
        for (int i = 0; i < right.keyCount; i++) {
            right.keys[i]         = node.keys[mid + 1 + i];
            right.children[i]     = node.children[mid + 1 + i];
        }
        right.children[right.keyCount] = node.children[node.keyCount];
        node.keyCount = mid;

        return new Object[]{promKey, right};
    }

    // ── RANGE SEARCH ─────────────────────────

    @SuppressWarnings("unchecked")
    public List<Route> rangeSearch(int min, int max) {
        List<Route> result = new ArrayList<>();
        BPlusNode leaf = findFirstLeaf(root, min);

        while (leaf != null) {
            for (int i = 0; i < leaf.keyCount; i++) {
                if (leaf.keys[i] > max) return result;
                if (leaf.keys[i] >= min) {
                    result.addAll((List<Route>) leaf.children[i]);
                }
            }
            leaf = leaf.next;
        }
        return result;
    }

    private BPlusNode findFirstLeaf(BPlusNode node, int key) {
        if (node.isLeaf) return node;
        int i = 0;
        while (i < node.keyCount && key >= node.keys[i]) i++;
        return findFirstLeaf((BPlusNode) node.children[i], key);
    }

    // ── FASTEST ROUTES ────────────────────────

    @SuppressWarnings("unchecked")
    public List<Route> fastestRoutes(int n) {
        List<Route> result = new ArrayList<>();
        BPlusNode leaf = firstLeaf;
        while (leaf != null && result.size() < n) {
            for (int i = 0; i < leaf.keyCount && result.size() < n; i++) {
                result.addAll((List<Route>) leaf.children[i]);
            }
            leaf = leaf.next;
        }
        return result;
    }

    // ── HEIGHT ────────────────────────────────

    public int height() {
        int h = 0;
        BPlusNode n = root;
        while (!n.isLeaf) {
            n = (BPlusNode) n.children[0];
            h++;
        }
        return h + 1;
    }

    // ── PRETTY PRINT ─────────────────────────

    public void prettyPrint() {
        System.out.println("\n── B+ Tree orden 4 ──");
        prettyPrint(root, 0);
    }

    private void prettyPrint(BPlusNode node, int level) {
        StringBuilder sb = new StringBuilder("  ".repeat(level));
        sb.append(node.isLeaf ? "[H] " : "[I] ");
        for (int i = 0; i < node.keyCount; i++) {
            if (i > 0) sb.append(", ");
            sb.append(node.keys[i]);
        }
        System.out.println(sb);
        if (!node.isLeaf) {
            for (int i = 0; i <= node.keyCount; i++) {
                prettyPrint((BPlusNode) node.children[i], level + 1);
            }
        }
    }
}

// ──────────────────────────────────────────────
// CARGA CSV
// ──────────────────────────────────────────────
class CSVLoader {
    public static List<Route> loadEdges(String path) throws IOException {
        List<Route> routes = new ArrayList<>();
        BufferedReader br  = new BufferedReader(new FileReader(path));
        String line;
        boolean inEdges = false;

        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.equals("EDGES")) { inEdges = true; continue; }
            if (line.equals("NODES")) { inEdges = false; continue; }
            if (line.isEmpty() || line.startsWith("origen")) continue;

            if (inEdges) {
                String[] p = line.split(",");
                if (p.length < 6) continue;
                routes.add(new Route(
                    p[0].trim(), p[1].trim(),
                    Integer.parseInt(p[2].trim()),
                    p[5].trim().equalsIgnoreCase("true")
                ));
            }
        }
        br.close();
        return routes;
    }
}

// ──────────────────────────────────────────────
// MAIN
// ──────────────────────────────────────────────
public class p03_bplus_tree {

    static int passed = 0, failed = 0;

    static void check(String name, boolean cond, String detail) {
        if (cond) { System.out.println("✓ PASS  " + name); passed++; }
        else      { System.out.println("✗ FAIL  " + name + " → " + detail); failed++; }
    }

    public static void main(String[] args) throws IOException {
        String csv = args.length > 0 ? args[0] : "../data/sincelejo_v2.csv";

        List<Route> routes = CSVLoader.loadEdges(csv);
        System.out.println("Aristas cargadas: " + routes.size());

        BPlusTree bp = new BPlusTree();
        for (Route r : routes) {
            bp.insert(r.costo, r);
        }

        bp.prettyPrint();
        System.out.println("Altura B+ Tree: " + bp.height());

        // ── Tests ──
        System.out.println("\n── Tests ──");

        // TEST 1: altura válida
        check("Altura del B+ Tree es ≥ 1",
              bp.height() >= 1, "altura=" + bp.height());

        // TEST 2: rangeSearch retorna resultados
        List<Route> rango = bp.rangeSearch(0, 10);
        check("rangeSearch(0,10) retorna rutas",
              !rango.isEmpty(), "lista vacía");

        // TEST 3: rangeSearch respeta límites
        boolean dentroRango = rango.stream().allMatch(r -> r.costo <= 10);
        check("Todas las rutas en rangeSearch(0,10) tienen costo ≤ 10",
              dentroRango, "alguna ruta fuera de rango");

        // TEST 4: fastestRoutes retorna n rutas
        List<Route> fast = bp.fastestRoutes(5);
        check("fastestRoutes(5) retorna al menos 1 ruta",
              !fast.isEmpty(), "lista vacía");

        // TEST 5: fastestRoutes ordena por costo
        if (fast.size() >= 2) {
            boolean ordenado = fast.get(0).costo <= fast.get(fast.size()-1).costo;
            check("fastestRoutes devuelve rutas ordenadas por costo",
                  ordenado, fast.get(0).costo + " > " + fast.get(fast.size()-1).costo);
        }

        // Mostrar rutas más rápidas
        System.out.println("\nRutas más rápidas (top 5):");
        for (Route r : bp.fastestRoutes(5))
            System.out.println("  " + r);

        System.out.println("\nRutas con tiempo 0-10 min:");
        for (Route r : rango)
            System.out.println("  " + r);

        System.out.println("\n" + "=".repeat(42));
        System.out.println("  Resultados: " + passed + " passed  |  " + failed + " failed");
        System.out.println("=".repeat(42));
    }
}
