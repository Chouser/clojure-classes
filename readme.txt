The code in src/net/n01se/clojure_classes.clj uses Clojure, the
Clojure .java sources, and the Graphviz "dot" program to produce
"graph.dot".

The "graph.svg" file is then produced using:
dot -Tsvg < graph.dot > graph.svg

The "graph.png" file is produced by exporting "graph.svg" from
inkscape.

Inkskape is also used to add the legend and make other minor manual
adjustments (line thickness for example) seen in "graph-w-legend.svg"
and "graph-w-legend.png".

The .png's have also been run through gimp to reduce the number of
colors and therefore the file size.

--Chouser, Dec 2008
