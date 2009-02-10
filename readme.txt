The code in src/net/n01se/clojure_classes.clj uses Clojure, the
Clojure .java sources, and the Graphviz "dot" program to produce
"graph.dot".

I run it like:
clj -cp src/ -e "(use 'net.n01se.clojure-classes)" > graph.dot

The "graph.svg" file is then produced using:
dot -Tsvg < graph.dot > graph.svg

Adjustments to line thickness in graph.svg were made by:
sed -b 's/stroke:#\w*;/&stroke-width:1.5;/g' graph.svg

Inkskape is used to add the legend and make other minor manual
adjustments seen in "graph-w-legend.svg" and "graph-w-legend.png".

The graph-w-legend.png has also been run through gimp to reduce the
number of colors and therefore the file size.

--Chouser, Feb 2009
