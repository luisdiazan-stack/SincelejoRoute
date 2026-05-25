"""
P15 – Análisis de impacto SETP
SincelejoRoute – Estructura de Datos II
Corporación Universitaria Antonio José de Sucre – UAJS
Autor: [Tu nombre]
"""

import csv
import heapq
import math
from collections import defaultdict, deque


# ──────────────────────────────────────────────
# 1. CARGA DEL GRAFO DESDE CSV
# ──────────────────────────────────────────────

def load_graph(csv_path: str):
    """
    Carga nodos y aristas desde sincelejo_v2.csv.
    Retorna:
      nodes   : dict  id -> {"nombre", "tipo", "lat", "lon", "is_setp"}
      adj     : dict  id -> list[(vecino, tiempo_min, capacidad, is_setp)]
    """
    nodes = {}
    adj   = defaultdict(list)

    with open(csv_path, newline="", encoding="utf-8") as f:
        mode = None
        reader = csv.reader(f)
        for row in reader:
            if not row or not row[0].strip():
                continue
            tag = row[0].strip()
            if tag == "NODES":
                mode = "nodes"; next(reader); continue
            if tag == "EDGES":
                mode = "edges"; next(reader); continue

            if mode == "nodes":
                nid, nombre, tipo, lat, lon, is_setp = (x.strip() for x in row)
                nodes[nid] = {
                    "nombre":  nombre,
                    "tipo":    tipo,
                    "lat":     float(lat),
                    "lon":     float(lon),
                    "is_setp": is_setp.lower() == "true",
                }
            elif mode == "edges":
                orig, dest, t, cap, bidir, is_setp = (x.strip() for x in row)
                t, cap = int(t), int(cap)
                setp   = is_setp.lower() == "true"
                adj[orig].append((dest, t, cap, setp))
                if bidir.lower() == "true":
                    adj[dest].append((orig, t, cap, setp))

    return nodes, adj


# ──────────────────────────────────────────────
# 2. SUBGRAFO (con / sin SETP)
# ──────────────────────────────────────────────

def build_subgraph(nodes, adj, include_setp: bool):
    """
    Retorna un diccionario de adyacencia filtrado.
    - include_setp=True  → usa todos los nodos y aristas.
    - include_setp=False → excluye nodos SETP y aristas SETP.
    """
    active_nodes = {
        nid for nid, data in nodes.items()
        if include_setp or not data["is_setp"]
    }
    sub = defaultdict(list)
    for u in active_nodes:
        for (v, t, cap, setp) in adj[u]:
            if v in active_nodes and (include_setp or not setp):
                sub[u].append((v, t, cap, setp))
    return sub


# ──────────────────────────────────────────────
# 3. DIJKSTRA (distancias desde una fuente)
# ──────────────────────────────────────────────

def dijkstra(adj, src, node_ids):
    """Retorna dict nodo -> distancia mínima desde src."""
    dist = {n: math.inf for n in node_ids}
    dist[src] = 0
    heap = [(0, src)]
    while heap:
        d, u = heapq.heappop(heap)
        if d > dist[u]:
            continue
        for (v, t, *_) in adj[u]:
            nd = d + t
            if nd < dist[v]:
                dist[v] = nd
                heapq.heappush(heap, (nd, v))
    return dist


# ──────────────────────────────────────────────
# 4. MÉTRICAS PRINCIPALES
# ──────────────────────────────────────────────

def average_path_length(adj, node_ids) -> float:
    """
    Longitud media de todos los caminos mínimos finitos
    entre pares distintos de nodos.
    """
    total, count = 0.0, 0
    for src in node_ids:
        dists = dijkstra(adj, src, node_ids)
        for dst, d in dists.items():
            if dst != src and d != math.inf:
                total += d
                count += 1
    return total / count if count else math.inf


def network_diameter(adj, node_ids) -> float:
    """
    Distancia máxima entre cualquier par de nodos alcanzables.
    """
    diameter = 0.0
    for src in node_ids:
        dists = dijkstra(adj, src, node_ids)
        for d in dists.values():
            if d != math.inf:
                diameter = max(diameter, d)
    return diameter


def clustering_coefficient(adj, node_ids) -> float:
    """
    Coeficiente de agrupamiento promedio (no dirigido).
    Para cada nodo calcula triángulos / pares posibles entre vecinos.
    """
    coeffs = []
    node_set = set(node_ids)
    for u in node_ids:
        neighbors = {v for (v, *_) in adj[u] if v in node_set}
        k = len(neighbors)
        if k < 2:
            coeffs.append(0.0)
            continue
        triangles = sum(
            1 for v in neighbors
            for w in neighbors
            if v != w and any(nb == w for (nb, *_) in adj[v])
        )
        coeffs.append(triangles / (k * (k - 1)))
    return sum(coeffs) / len(coeffs) if coeffs else 0.0


def setp_benefit_score(metrics_before: dict, metrics_after: dict) -> float:
    """
    Índice compuesto de mejora SETP (0-100).
    Combina mejora en longitud media, diámetro y clustering.
    """
    def pct_change(before, after):
        if before == 0 or before == math.inf:
            return 0.0
        return (before - after) / before * 100  # positivo = mejora

    delta_apl = pct_change(metrics_before["avg_path"], metrics_after["avg_path"])
    delta_dia = pct_change(metrics_before["diameter"], metrics_after["diameter"])
    delta_cc  = (metrics_after["clustering"] - metrics_before["clustering"]) * 100

    score = 0.5 * delta_apl + 0.3 * delta_dia + 0.2 * delta_cc
    return round(max(0.0, score), 2)


# ──────────────────────────────────────────────
# 5. REPORTE DE IMPACTO
# ──────────────────────────────────────────────

def impact_report(csv_path: str) -> dict:
    """
    Función principal del módulo.
    Calcula métricas antes y después del SETP y genera el reporte.
    """
    nodes, adj = load_graph(csv_path)

    # Subgrafo SIN nodos SETP
    adj_no_setp = build_subgraph(nodes, adj, include_setp=False)
    ids_no_setp = [n for n, d in nodes.items() if not d["is_setp"]]

    # Subgrafo CON nodos SETP (red completa)
    adj_setp    = build_subgraph(nodes, adj, include_setp=True)
    ids_setp    = list(nodes.keys())

    print("Calculando métricas SIN SETP...")
    apl_before = average_path_length(adj_no_setp, ids_no_setp)
    dia_before = network_diameter(adj_no_setp, ids_no_setp)
    cc_before  = clustering_coefficient(adj_no_setp, ids_no_setp)

    print("Calculando métricas CON SETP...")
    apl_after  = average_path_length(adj_setp, ids_setp)
    dia_after  = network_diameter(adj_setp, ids_setp)
    cc_after   = clustering_coefficient(adj_setp, ids_setp)

    metrics_before = {"avg_path": apl_before, "diameter": dia_before, "clustering": cc_before}
    metrics_after  = {"avg_path": apl_after,  "diameter": dia_after,  "clustering": cc_after}
    score = setp_benefit_score(metrics_before, metrics_after)

    def pct(a, b):
        if a == math.inf or a == 0: return "N/A"
        return f"{(a - b) / a * 100:+.1f}%"

    report = {
        "antes": metrics_before,
        "despues": metrics_after,
        "cambio_avg_path":  pct(apl_before, apl_after),
        "cambio_diameter":  pct(dia_before, dia_after),
        "cambio_clustering": f"{(cc_after - cc_before)*100:+.1f}%",
        "setp_benefit_score": score,
        "nodos_total": len(ids_setp),
        "nodos_sin_setp": len(ids_no_setp),
        "nodos_setp": len(ids_setp) - len(ids_no_setp),
    }

    # Imprimir reporte
    _print_report(report)
    return report


def _print_report(r: dict):
    sep = "=" * 55
    print(f"\n{sep}")
    print("  REPORTE DE IMPACTO SETP – SincelejoRoute P15")
    print(sep)
    print(f"  Nodos totales        : {r['nodos_total']}")
    print(f"  Nodos universitarios : {r['nodos_sin_setp']}")
    print(f"  Nodos SETP añadidos  : {r['nodos_setp']}")
    print(sep)
    print(f"  {'Métrica':<30} {'Sin SETP':>9} {'Con SETP':>9} {'Cambio':>8}")
    print(f"  {'-'*52}")

    b, a = r["antes"], r["despues"]
    print(f"  {'Longitud media camino':<30} {b['avg_path']:>9.2f} {a['avg_path']:>9.2f} {r['cambio_avg_path']:>8}")
    print(f"  {'Diámetro de red':<30} {b['diameter']:>9.2f} {a['diameter']:>9.2f} {r['cambio_diameter']:>8}")
    print(f"  {'Coef. agrupamiento':<30} {b['clustering']:>9.4f} {a['clustering']:>9.4f} {r['cambio_clustering']:>8}")
    print(sep)
    print(f"  Índice de beneficio SETP : {r['setp_benefit_score']:.2f} / 100")
    print(sep)


# ──────────────────────────────────────────────
# 6. PUNTO DE ENTRADA
# ──────────────────────────────────────────────

if __name__ == "__main__":
    import sys
    csv_file = sys.argv[1] if len(sys.argv) > 1 else "data/sincelejo_v2.csv"
    impact_report(csv_file)
