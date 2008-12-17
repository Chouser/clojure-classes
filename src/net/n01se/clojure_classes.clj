; clojure-classes.clj - produces graphviz dot graph for Clojure Java classes
;   Copyright (c) Chris Houser, Dec 2008. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns net.n01se.clojure-classes
  (:import (java.lang Runtime)
           (java.io OutputStreamWriter InputStreamReader
                    ByteArrayOutputStream)
           (javax.swing JFrame JLabel ImageIcon)
           (clojure.lang PersistentQueue)))

(def srcpath "/home/chouser/build/clojure/src/jvm/clojure/lang/")

(defn system [cmd writedata]
      (let [proc (.exec (Runtime/getRuntime) (into-array cmd))
            isr  (InputStreamReader. (.getInputStream proc) "ISO-8859-1")
            osr  (OutputStreamWriter. (.getOutputStream proc))
            baos (ByteArrayOutputStream.)]
        (.write osr writedata)
        (.close osr)
        (loop [c (.read isr)]
          (if (neg? c)
            (do (.waitFor proc)
                (.toByteArray baos))
            (do (.write baos c)
                (recur (.read isr)))))))

(defmacro str-for [& for-stuff]
  `(apply str (for ~@for-stuff)))

(def colorlist (cycle ["#aa0000" "#00aa00" "#0000aa" "#000000" "#888888"
                       "#008888" "#880088" "#888800"]))

(def preds '{ISeq seq?, IPersistentMap map?, IPersistentVector vector?,
             Symbol symbol?, Keyword keyword?, Var var?,
             IPersistentCollection coll?, IPersistentList list?,
             IPersistentSet set?, Number number?, IFn ifn?,
             Associative associative?, Sequential sequential?,
             Sorted sorted?, Reversible reversible?, Ratio ratio?
             Fn fn?, Delay delay?})

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
             MultiFn defmulti Keyword keyword Symbol "symbol, gensym"
             AtomicReference$IRef "atom"})

(def aliases '{AtomicReference$IRef ""})

(def extra-seed-classes [
  ;clojure.proxy.java.util.concurrent.atomic.AtomicReference$IRef
  ])

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
  (.getSimpleName cls))

(defn class-label [cls]
  (let [clsname (class-name cls)
        a (aliases (symbol clsname) clsname)
        pred (preds (symbol clsname))
        ctor (ctors (symbol clsname))]
    (str a
         ;(when ctor (str (when-not (empty? a) "\\n") ctor))
         (when pred (str \\ \n pred)))))

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
            kids (filter class-filter (bases cls))]
        (recur (assoc found cls kids)
               (into (pop work) (remove found kids)))))))

(def classes (sort-by #(.getSimpleName %) (keys graph)))

(def dotstr
  (str
    "digraph {\n"
    "  rankdir=LR;\n"
    "  dpi=55;\n"
    "  nodesep=0.10;\n"
    "  node[ fontname=Helvetica shape=box ];\n"
    (str-for [[cls color] (map list classes colorlist)]
      (str "  \"" cls "\" [ label=\"" (class-label cls) "\" "
           "color=\"" color "\" "
           "shape=\"" (choose-shape cls) "\"];\n"
           (str-for [sub (graph cls)]
             (str "  \"" cls "\" -> \"" sub "\""
                  " [ color=\"" color "\" ];\n"))))
    "}\n"))

(print dotstr)

(def png (system ["dot" "-Tpng"] dotstr))

(doto (JFrame. "Clojure Classes")
  (.add (JLabel. (ImageIcon. png)))
  (.setVisible true))
