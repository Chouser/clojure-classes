; clojure-classes.clj - produces graphviz dot graph for Clojure Java classes
;   Copyright (c) Chris Houser, Dec 2008. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns net.n01se.clojure-classes
  (:use [clojure.contrib.shell-out :only (sh)])
  (:import (java.lang Runtime)
           (java.io OutputStreamWriter InputStreamReader
                    ByteArrayOutputStream)
           (javax.swing JFrame JLabel JScrollPane ImageIcon)
           (clojure.lang PersistentQueue)))

(def srcpath "/home/chouser/build/clj/trunk/src/jvm/clojure/lang/")

(defmacro str-for [& for-stuff]
  `(apply str (for ~@for-stuff)))

(def colors ["#d70000" "#d7009e" "#b300d7" "#5a00d7" "#0061d7" "#00d0d7"
             "#00d764" "#76d700" "#d78100"])
; Some lighter colors:
; "#ff817f" "#ff7fea" "#b47fff" "#7fa5ff" "#7ffffb" "#a8ff7f" "#ffd97f"

(def preds '{ISeq seq?, IPersistentMap map?, IPersistentVector vector?,
             Symbol symbol?, Keyword keyword?, Var var?,
             IPersistentCollection coll?, IPersistentList list?,
             IPersistentSet set?, Number number?, IFn ifn?,
             Associative associative?, Sequential sequential?,
             Sorted sorted?, Reversible reversible?, Ratio ratio?
             Fn fn?, Delay delay?, Class class?, BigDecimal decimal?,
             String string?})

(def ctors '{IteratorSeq iterator-seq PersistentList list ISeq seq
             EnumerationSeq enumeration-seq Var "intern, with-local-vars"
             LazilyPersistentVector "vector, vec"
             PersistentHashMap hash-map PersistentHashSet "hash-set, set"
             PersistentArrayMap array-map
             PersistentTreeMap "sorted-map, sorted-map-by"
             PersistentTreeSet sorted-set
             PersistentStructMap$Def create-struct
             PersistentStructMap "struct-map, struct"
             LazyCons lazy-cons Range range FnSeq fnseq
             MultiFn defmulti Keyword keyword Symbol "symbol, gensym"})

(def clusters '#{})

(def badges
  '{IMeta M Iterable T Counted 1 Streamable S
    Reversible R Named N Comparable =})

(def color-override '{PersistentList "#76d700" PersistentQueue "#0061d7"
                      LazySeq "#d78100"})

(def aliases '{Object$Future$IDeref "(future)"})

(def extra-seed-classes [clojure.proxy.java.lang.Object$Future$IDeref])

(defn class-filter [cls]
  (let [package (-> cls .getPackage .getName)]
    (or (= package "clojure.lang")
        (and (.startsWith package "java") (.isInterface cls)))))

(defn choose-shape [cls]
  (cond
    (not (-> cls .getPackage .getName (.startsWith "clojure"))) "diamond"
    (.isInterface cls) "octagon"
    :else "oval"))

(defn class-name [cls]
  (symbol (.getSimpleName cls)))

(defn class-label [cls]
  (let [clsname (class-name cls)
        a (aliases clsname (str clsname))
        pred (preds clsname)
        ctor (ctors clsname)
        anc (set (map class-name (ancestors cls)))]
    (str a
         ;(when ctor (str (when-not (empty? a) "\\n") ctor))
         (when pred (str \\ \n pred))
         (when-let [badge (seq (filter identity (map badges (map anc (keys badges)))))]
           (str "\\n[" (apply str badge) "]")))))

(defn class-color [cls]
  (color-override (class-name cls)
    (nth colors (rem (Math/abs (hash (str cls))) (count colors)))))

(def graph
  (loop [found {}
         work (into
                (into PersistentQueue/EMPTY extra-seed-classes)
                (filter #(and % (some class-filter (bases %)))
                        (for [file (.listFiles (java.io.File. srcpath))]
                          (let [[cname ext] (.split (.getName file) "\\.")]
                            (when (= ext "java")
                              (Class/forName (str "clojure.lang." cname)))))))]
    (if (empty? work)
      found
      (let [cls (peek work)
            kids (seq (filter class-filter (bases cls)))]
        (recur (assoc found cls kids)
               (into (pop work) (remove found kids)))))))

(def classes (sort-by #(.getSimpleName %) (keys graph)))

(def dotstr
  (str
    "digraph {\n"
    "  rankdir=LR;\n"
    "  dpi=55;\n"
    "  nodesep=0.10;\n"
    "  ranksep=1.2;\n"
    "  mclimit=2500.0;\n"
    ;"  splines=true;\n"
    ;"  overlap=scale;\n"
    "  node[ fontname=Helvetica shape=box ];\n"
    "
  subgraph cluster_legend {
    label=\"Legend\"
    fontname=\"Helvetica Bold\"
    fontsize=19
    bgcolor=\"#dddddd\"
    \"Clojure Interface\" [ shape=octagon fillcolor=\"#ffffff\" style=filled ];
    \"Java Interface\" [ shape=diamond fillcolor=\"#ffffff\" style=filled ];
    \"Clojure class\" [ shape=oval fillcolor=\"#ffffff\" style=filled ];
    "
    (when (seq badges)
      (str "
    badges [
      shape=record
      style=filled
      fillcolor=\"#ffffff\"
      label=\"{{"
       (apply str (interpose "|" (vals badges)))
      "}|{"
       (apply str (interpose "|" (keys badges)))
      "}}\"
    ]"))
    "
  }
"
    (str-for [cls classes]
      (when-not (badges (class-name cls))
        (let [color (class-color cls)
              node (str "  \"" cls "\" [ label=\"" (class-label cls) "\" "
                        "color=\"" color "\" "
                        "shape=\"" (choose-shape cls) "\"];\n")
              cluster (some #(clusters (class-name %))
                            (cons cls (ancestors cls)))]
          (str (when cluster (str "subgraph cluster_" cluster " {\n"))
              node
              (when cluster "}\n")
              (str-for [sub (graph cls)]
                (when-not (badges (class-name sub))
                  (str "  \"" cls "\" -> \"" sub "\""
                       " [ color=\"" color "\" ];\n")))))))
    "}\n"))

(print dotstr)

(doto (JFrame. "Clojure Classes")
  (.add (-> (sh "dot" "-Tpng" :in dotstr :out :bytes) ImageIcon.
          JLabel. JScrollPane.))
  (.setDefaultCloseOperation javax.swing.WindowConstants/DISPOSE_ON_CLOSE)
  (.setVisible true))
