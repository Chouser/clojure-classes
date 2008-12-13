(ns net.n01se.java2dot
  (:import (java.lang Runtime)
           (java.io OutputStreamWriter InputStreamReader
                    ByteArrayOutputStream)
           (javax.swing JFrame JLabel ImageIcon)))

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

(def predmap '{ISeq seq?, IPersistentMap map?, IPersistentVector vector?,
               Symbol symbol?, Keyword keyword?, Var var?,
               IPersistentCollection coll?, IPersistentList list?,
               IPersistentSet set?, Number number?, IFn ifn?, Fn fn?,
               Associative associative?, Sequential sequential?,
               Sorted sorted?, Reversible reversible?, Delay delay?,
               Ratio ratio?})

(def dotstr
  (binding [colorlist colorlist]
    (str
      "digraph {\n"
      "  rankdir=LR;\n"
      "  dpi=55;\n"
      "  nodesep=0.10;\n"
      "  node[ fontname=Helvetica ];\n"
      (str-for [file (.listFiles (java.io.File. srcpath))]
        (let [[cname ext] (.split (.getName file) "\\.")]
          (when (= ext "java")
            (set! colorlist (rest colorlist))
            (let [[color] colorlist
                  cls (Class/forName (str "clojure.lang." cname))
                  clsname (.getSimpleName cls)
                  pred (predmap (symbol clsname))
                  label (if pred (str \" clsname \\ \n pred \") clsname)
                  cljbases (filter #(= (-> % .getPackage .getName)
                                       "clojure.lang")
                                   (bases cls))]
              (when (or cljbases pred)
                (str "  " clsname " [ label=" label " color=\"" color "\" ];\n"
                     (str-for [sub cljbases]
                        (str "  " clsname " -> " (.getSimpleName sub)
                             " [ color=\"" color "\" ];\n"))))))))
      "}\n")))

(print dotstr)

(def png (system ["dot" "-Tpng"] dotstr))

(doto (JFrame. "Clojure Classes")
  (.add (JLabel. (ImageIcon. png)))
  (.setVisible true))
