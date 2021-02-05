(ns ^:no-doc lread.test-doc-blocks.impl.zutil
  "Misc rewrite-cljc zipper utils."
  (:require [rewrite-clj.zip :as z]))

(defn zdepth [zloc]
  (->> (z/up zloc)
       (iterate z/up)
       (take-while identity)
       count))

(defn remove-n-siblings-right
  "Return `zloc` with `cnt` siblings to the right of the current node removed.
  Zipper remains location remains unchanged.

  Note: untested for resulting empty list.
  Aside: this was surprisingly difficult to write."
  [zloc cnt]
  (let [orig-depth (zdepth zloc)
        start-at (z/right zloc)
        sibs-available (count (->> (iterate z/right start-at)
                                   (take-while identity)))]
    (loop [zloc-del start-at
           del-cnt  (min cnt sibs-available)]
      (if (<= del-cnt 0)
        (let [zloc-ret (if (< cnt sibs-available)
                         (z/left zloc-del)
                         zloc-del)]
          (if (> (zdepth zloc-ret) orig-depth)
            (->> (iterate z/up zloc-ret)
                 (take-while #(>= (zdepth %) orig-depth))
                 last)
            zloc-ret))
        (recur (-> zloc-del
                   z/remove
                   z/next)
               (dec del-cnt))))))


(comment
  (->"one [two [fa fee]] [three four] five six seven"
     z/of-string
     z/right
     (remove-n-siblings-right 4)
     z/string)







  )
