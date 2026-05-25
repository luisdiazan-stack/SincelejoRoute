# SincelejoRoute – Sistema Integrado de Rutas Universitarias y SETP

**Corporación Universitaria Antonio José de Sucre – UAJS**
Facultad de Ingeniería · Ingeniería de Sistemas · Estructura de Datos II
Período: 2025-I · Docente: Ing. Jaider Enrique Reyes Herazo, M.Sc.

---

## Descripción

Sistema que modela la red de movilidad de Sincelejo como un **grafo pesado dirigido** de 20 nodos y 34 aristas, integrando la infraestructura universitaria (UAJS, UNISUCRE, CECAR) con los nodos reales del SETP Metro Sabanas.

El argumento central: demostrar con algoritmos reales que la Estación de Transferencia permite atravesar toda la ciudad con un solo pasaje, reduciendo la longitud media de camino en al menos 20% y aumentando el flujo máximo de pasajeros en 90%.

---

## Estructura del repositorio

```
SincelejoRoute/
├── modulo_A/          # Bloque A – Estructuras de árbol
│   ├── p01_avl_tree.java
│   ├── p02_btree.py
│   ├── p03_bplus_tree.java
│   ├── p04_accessibility.py
│   └── p05_benchmark.py
├── modulo_B/          # Bloque B – Algoritmos de grafos
│   ├── p06_graph.py
│   ├── p07_dijkstra.py
│   ├── p08_bellman_ford.java
│   ├── p09_prim_mst.py
│   ├── p10_kruskal.java
│   ├── p11_max_flow.java
│   └── p12_pert_cpm.py
├── modulo_C/          # Bloque C – Proyecto, SETP e integración
│   ├── p13_minimax.py
│   ├── p14_nash.java
│   ├── p15_impact.py
│   ├── p16_integration.py
│   └── p17_cli.py
├── data/
│   └── sincelejo_v2.csv
└── README.md
```

---

## Datos del grafo

El grafo se alimenta del archivo `data/sincelejo_v2.csv` con dos secciones: `NODES` y `EDGES`.

**Ningún módulo puede hardcodear datos del grafo. Todo se carga desde este CSV.**

### Nodos (20)
| Categoría | Nodos |
|---|---|
| Universidades | N00, N01, N02, N03 |
| IPS / Salud | N04, N05, N06 |
| SETP Metro Sabanas | N07 (Central), N09, N10, N11, N12, N15 |
| Movilidad vial | N08, N13, N14 |
| Referencia/Comercio | N16, N17, N18, N19 |

### Aristas
Cada arista tiene: `tiempo_min`, `capacidad` (personas/hora), `is_setp` (booleano).

---

## Módulos por pareja

| Pareja | Módulo | Lenguaje | Descripción |
|---|---|---|---|
| P01 | AVL Tree | Java | Árbol AVL con rotaciones LL/RR/LR/RL |
| P02 | B-Tree orden 3 | Python | B-Tree con split |
| P03 | B+ Tree orden 4 | Java | B+ con promoción y rangeSearch |
| P04 | Accesibilidad B+ | Python | AccessAnalyzer sobre B+ de P03 |
| P05 | Benchmark árboles | Java+Python | Comparativo AVL vs B vs B+ |
| P06 | Grafo y recorridos | Python | SincelejoGraph con BFS/DFS/Dijkstra |
| P07 | Dijkstra SETP | Python | Rutas preferentes SETP, all_pairs |
| P08 | Bellman-Ford | Java | Descuento SETP, detección ciclos neg. |
| P09 | Prim MST | Python | MST con prioridad SETP |
| P10 | Kruskal + Union-Find | Java | MST con compresión de camino |
| P11 | Flujo máximo Edmonds-Karp | Java | Flujo max N01→N08, capacity_boost |
| P12 | PERT/CPM | Python | Ruta crítica del proyecto |
| P13 | Minimax + alfa-beta | Python | Selección de ruta como juego |
| P14 | Equilibrio Nash | Java | Nash en selección de corredor |
| **P15** | **Análisis impacto SETP** | **Python** | **Métricas antes/después SETP** |
| P16 | Integración A+B | Python | SincelejoCore: AVL + Dijkstra + CLI |
| P17 | CLI Final | Python/Java | CLI interactiva con todas las opciones |

---

## Interfaz pública de P15

```python
from modulo_C.p15_impact import (
    load_graph,               # (csv_path) -> nodes, adj
    average_path_length,      # (adj, node_ids) -> float
    network_diameter,         # (adj, node_ids) -> float
    clustering_coefficient,   # (adj, node_ids) -> float
    setp_benefit_score,       # (metrics_before, metrics_after) -> float
    impact_report,            # (csv_path) -> dict
)
```

---

## Cómo ejecutar

### P15 – Análisis de impacto SETP (Python)
```bash
cd modulo_C
python p15_impact.py ../data/sincelejo_v2.csv
```

### Tests de P15
```bash
cd modulo_C
python -m pytest test_p15.py -v
# o sin pytest:
python test_p15.py
```

---

## Normas del repositorio

- Mínimo **2 commits por semana** por pareja
- Cada módulo debe incluir al menos **3 casos de prueba**
- **Ninguna pareja accede a la implementación interna de otro módulo**, solo a la interfaz pública
- Java: convenciones Oracle · Python: PEP 8
- Sin código comentado en el entregable final

---

## Condición especial de aprobación

La demo en vivo de la semana 8 debe demostrar reducción ≥ 20% en longitud media de camino con SETP activo. Si la CLI no ejecuta correctamente en la demo, el puntaje de los criterios 3 y 4 se reduce al 50%.
