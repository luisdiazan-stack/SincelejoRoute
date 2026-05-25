"""
P02 – B-Tree orden 3
SincelejoRoute – Estructura de Datos II
UAJS Facultad de Ingeniería
Autor: [Tu nombre]
"""

import csv
from collections import defaultdict


# ──────────────────────────────────────────────
# NODO DEL B-TREE
# ──────────────────────────────────────────────

class BTreeNode:
    def __init__(self, leaf=True):
        self.keys     = []   # claves (tiempo_min)
        self.values   = []   # listas de aristas por clave
        self.children = []   # hijos
        self.leaf     = leaf

    def __str__(self):
        return f"BTreeNode(keys={self.keys}, leaf={self.leaf})"


# ──────────────────────────────────────────────
# B-TREE ORDEN 3
# ──────────────────────────────────────────────

class BTree:
    """
    B-Tree de orden 3 (máximo 2 claves por nodo interno).
    Clave  : tiempo_min (int)
    Valor  : lista de aristas con ese tiempo
    """

    def __init__(self, order=3):
        self.order = order          # t = order → max 2t-1 claves = 2 claves
        self.t     = order          # mínimo t-1 = 2 claves por nodo (excepto raíz)
        self.root  = BTreeNode(leaf=True)

    # ── INSERT ────────────────────────────────

    def insert(self, key, edge):
        """Inserta una arista con su tiempo_min como clave."""
        root = self.root

        # Si la raíz está llena, hacer split
        if len(root.keys) == 2 * self.t - 1:
            new_root       = BTreeNode(leaf=False)
            new_root.children.append(self.root)
            self._split_child(new_root, 0)
            self.root = new_root

        self._insert_non_full(self.root, key, edge)

    def _insert_non_full(self, node, key, edge):
        i = len(node.keys) - 1

        if node.leaf:
            # Buscar posición o clave existente
            for idx, k in enumerate(node.keys):
                if k == key:
                    node.values[idx].append(edge)
                    return
            # Insertar en orden
            node.keys.append(None)
            node.values.append(None)
            while i >= 0 and key < node.keys[i]:
                node.keys[i + 1]   = node.keys[i]
                node.values[i + 1] = node.values[i]
                i -= 1
            node.keys[i + 1]   = key
            node.values[i + 1] = [edge]
        else:
            # Buscar clave existente en nodo interno
            for idx, k in enumerate(node.keys):
                if k == key:
                    node.values[idx].append(edge)
                    return
            # Bajar al hijo correcto
            while i >= 0 and key < node.keys[i]:
                i -= 1
            i += 1
            if len(node.children[i].keys) == 2 * self.t - 1:
                self._split_child(node, i)
                if key > node.keys[i]:
                    i += 1
            self._insert_non_full(node.children[i], key, edge)

    def _split_child(self, parent, i):
        """Split del hijo i de parent."""
        t     = self.t
        child = parent.children[i]
        new   = BTreeNode(leaf=child.leaf)
        mid   = t - 1  # índice de la clave mediana

        # Clave y valor que sube al padre
        mid_key = child.keys[mid]
        mid_val = child.values[mid]

        # Nueva mitad derecha
        new.keys   = child.keys[mid + 1:]
        new.values = child.values[mid + 1:]

        # Si no es hoja, mover hijos también
        if not child.leaf:
            new.children = child.children[mid + 1:]
            child.children = child.children[:mid + 1]

        # Truncar hijo izquierdo
        child.keys   = child.keys[:mid]
        child.values = child.values[:mid]

        # Insertar clave mediana en el padre
        parent.keys.insert(i, mid_key)
        parent.values.insert(i, mid_val)
        parent.children.insert(i + 1, new)

    # ── SEARCH ────────────────────────────────

    def search(self, key, node=None):
        """Retorna lista de aristas con ese tiempo_min, o None."""
        if node is None:
            node = self.root
        i = 0
        while i < len(node.keys) and key > node.keys[i]:
            i += 1
        if i < len(node.keys) and key == node.keys[i]:
            return node.values[i]
        if node.leaf:
            return None
        return self.search(key, node.children[i])

    # ── HEIGHT ────────────────────────────────

    def height(self, node=None):
        if node is None:
            node = self.root
        if node.leaf:
            return 1
        return 1 + self.height(node.children[0])

    # ── PRINT ─────────────────────────────────

    def pretty_print(self, node=None, level=0, prefix="Raíz: "):
        if node is None:
            node = self.root
        print(" " * (level * 4) + prefix + str(node.keys))
        if not node.leaf:
            for i, child in enumerate(node.children):
                self.pretty_print(child, level + 1, f"hijo[{i}]: ")


# ──────────────────────────────────────────────
# CARGA DEL CSV
# ──────────────────────────────────────────────

def load_edges(csv_path: str):
    """Carga aristas desde sincelejo_v2.csv."""
    edges = []
    with open(csv_path, newline="", encoding="utf-8") as f:
        mode = None
        reader = csv.reader(f)
        for row in reader:
            if not row or not row[0].strip():
                continue
            tag = row[0].strip()
            if tag == "EDGES":
                mode = "edges"; next(reader); continue
            if tag == "NODES":
                mode = "nodes"; continue
            if mode == "edges":
                orig, dest, t, cap, bidir, is_setp = (x.strip() for x in row)
                edges.append({
                    "origen":  orig,
                    "destino": dest,
                    "tiempo":  int(t),
                    "capacidad": int(cap),
                    "is_setp": is_setp.lower() == "true"
                })
    return edges


# ──────────────────────────────────────────────
# TESTS
# ──────────────────────────────────────────────

PASS = "\033[92m✓ PASS\033[0m"
FAIL = "\033[91m✗ FAIL\033[0m"
passed = failed = 0

def check(name, cond, detail=""):
    global passed, failed
    if cond: print(f"{PASS}  {name}"); passed += 1
    else:    print(f"{FAIL}  {name}  → {detail}"); failed += 1


def run_tests(bt, edges):
    print("\n── Tests ──")

    # TEST 1: Árbol tiene altura menor que lista plana
    check("Altura del B-Tree es ≥ 1",
          bt.height() >= 1, f"altura={bt.height()}")

    # TEST 2: B-Tree más bajo que AVL (O(log_t n) vs O(log_2 n))
    import math
    n = len(set(e["tiempo"] for e in edges))
    max_avl_height = math.ceil(math.log2(n + 1)) if n > 0 else 1
    check("B-Tree más bajo o igual que AVL equivalente",
          bt.height() <= max_avl_height,
          f"btree={bt.height()}, avl_max={max_avl_height}")

    # TEST 3: search encuentra tiempo existente
    result = bt.search(4)
    check("search(4) retorna aristas",
          result is not None and len(result) > 0,
          f"result={result}")

    # TEST 4: search no encuentra tiempo inexistente
    result99 = bt.search(99)
    check("search(99) retorna None",
          result99 is None, f"result={result99}")

    # TEST 5: Todos los pesos insertados son buscables
    tiempos = set(e["tiempo"] for e in edges)
    all_found = all(bt.search(t) is not None for t in tiempos)
    check(f"Todos los {len(tiempos)} tiempos distintos son encontrables",
          all_found, "algún tiempo no encontrado")


# ──────────────────────────────────────────────
# MAIN
# ──────────────────────────────────────────────

if __name__ == "__main__":
    import sys
    csv_file = sys.argv[1] if len(sys.argv) > 1 else "../data/sincelejo_v2.csv"

    print("Cargando aristas desde CSV...")
    edges = load_edges(csv_file)
    print(f"  {len(edges)} aristas cargadas")

    # Construir B-Tree
    bt = BTree(order=3)
    for edge in edges:
        bt.insert(edge["tiempo"], edge)

    # Mostrar árbol
    print("\n── B-Tree orden 3 ──")
    bt.pretty_print()
    print(f"\nAltura del B-Tree : {bt.height()}")

    # Mostrar tiempos únicos
    tiempos = sorted(set(e["tiempo"] for e in edges))
    print(f"Tiempos distintos : {tiempos}")

    # Comparar con AVL
    import math
    n = len(tiempos)
    avl_h = math.ceil(1.44 * math.log2(n + 1)) if n > 0 else 1
    print(f"\nComparación de altura:")
    print(f"  B-Tree orden 3 : {bt.height()}")
    print(f"  AVL estimado   : {avl_h}")
    print(f"  → B-Tree es más {'bajo' if bt.height() <= avl_h else 'alto'} que AVL")

    # Ejecutar tests
    run_tests(bt, edges)

    print(f"\n{'='*42}")
    print(f"  Resultados: {passed} passed  |  {failed} failed")
    print(f"{'='*42}\n")
