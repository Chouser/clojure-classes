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

(def dotstr (str
  "digraph {\n"
  "  rankdir=LR;\n"
  "  dpi=55;\n"
  "  nodesep=0.10;\n"
  "  node[ fontname=Helvetica ];\n"
  (str-for [file (. (new java.io.File srcpath) (listFiles))]
    (when-not (. file (isDirectory))
      (with-open rdr (new java.io.BufferedReader (new java.io.FileReader file))
        (str-for [line (line-seq rdr)]
          (let [[line name subs]
                  (re-matches #"public.* (?:class|interface) (\\w+) (.*)" line)]
            (when name
              (let [[color] colorlist]
                (def colorlist (rest colorlist))
                (str "  " name " [ color=\"" color "\" ];\n"
                  (str-for [[subline sub] (re-seq #"(\\w+)(?:<[^>]*>)?" subs)]
                    (when-not (#{"extends" "implements"} sub)
                              (str "  " name " -> " sub
                                  " [ color=\"" color "\" ];\n")))))))))))
  "}\n"))

;(print dotstr)

(def png (system ["dot" "-Tpng"] dotstr))

(doto (new javax.swing.JFrame "Clojure Classes")
  (add (new javax.swing.JLabel (new javax.swing.ImageIcon png)))
  (setVisible true))
