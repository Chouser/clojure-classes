(def srcpath "/home/chouser/build/clojure/src/jvm/clojure/lang/")

(defn system [cmd writedata]
      (let [proc (.. java.lang.Runtime (getRuntime) (exec (into-array cmd)))
            isr  (new java.io.InputStreamReader
                      (. proc (getInputStream)) "ISO-8859-1")
            osr  (new java.io.OutputStreamWriter (. proc (getOutputStream)))
            baos (new java.io.ByteArrayOutputStream)]
        (. osr (write writedata))
        (. osr (close))
        (loop [c (. isr (read))]
          (if (neg? c)
            (do (. proc (waitFor))
                (. baos (toByteArray)))
            (do (. baos (write c))
                (recur (. isr (read))))))))

(defmacro str-for [& for-stuff]
  `(apply str (for ~@for-stuff)))

(def colorlist (cycle ["#aa0000" "#00aa00" "#0000aa" "#000000" "#888888"
                       "#008888" "#880088" "#888800"]))

(def predmap '{ISeq seq? IPersistentMap map? IPersistentVector vector?
               Symbol symbol? Keyword keyword? Var var?
               IPersistentCollection coll? IPersistentList list?
               IPersistentSet set? Number number? IFn fn?
               Associative associative? Sequential sequential?
               Sorted sorted? Reversible reversible?})

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

(doto (new javax.swing.JFrame "Clojure Classes")
  (add (new javax.swing.JLabel (new javax.swing.ImageIcon png)))
  (setVisible true))
