(ns hipo.core-test
  (:require [cemerick.cljs.test :as test]
            [hipo.core :as hipo]
            [hipo.interceptor :refer [Interceptor]]
            cljsjs.document-register-element)
  (:require-macros [cemerick.cljs.test :refer [deftest is]]))

(deftest simple
  (is (= "B" (.-tagName (first (hipo/create [:b])))))
  (let [[e _] (hipo/create [:span "some text"])]
    (is (= "SPAN" (.-tagName e)))
    (is (= "some text" (.-textContent e)))
    (is (= js/document.TEXT_NODE (-> e .-childNodes (aget 0) .-nodeType)))
    (is (zero? (-> e .-children .-length))))
  (let [[e _] (hipo/create [:script {:src "http://somelink"}])]
    (is (= "" (.-textContent e)))
    (is (= "http://somelink" (.getAttribute e "src"))))
  (let [[e _] (hipo/create [:a {:href "http://somelink"} "anchor"])]
    (is (-> e .-tagName (= "A")))
    (is (= "anchor" (.-textContent e)))
    (is (= "http://somelink" (.getAttribute e "href"))))
  (let [a (atom 0)
        next-id #(swap! a inc)
        [e _] (hipo/create [:span {:attr (next-id)}])]
    (is (= "1" (.getAttribute e "attr"))))
  (let [[e _] (hipo/create [:div#id {:class "class1 class2"}])]
    (is (= "class1 class2" (.-className e))))
  (let [[e _] (hipo/create [:div#id.class1 {:class "class2 class3"}])]
    (is (= "class1 class2 class3" (.-className e))))
  (let [cs "class1 class2"
        [e _] (hipo/create [:div ^:attrs (merge {} {:class cs})])]
    (is (= "class1 class2" (.-className e))))
  (let [cs "class2 class3"
        [e _] (hipo/create [:div (list [:div#id.class1 {:class cs}])])]
    (is (= "class1 class2 class3" (.-className (.-firstChild e)))))
  (let [[e _] (hipo/create [:div.class1 ^:attrs (merge {:data-attr ""} {:class "class2 class3"})])]
    (is (= "class1 class2 class3" (.-className e))))
  (let [[e _] (hipo/create [:div (interpose [:br] (repeat 3 "test"))])]
    (is (= 5 (.. e -childNodes -length)))
    (is (= "test" (.. e -firstChild -textContent))))
  (let [[e _] (hipo/create [:div.class1 [:span#id1 "span1"] [:span#id2 "span2"]])]
    (is (= "span1span2" (.-textContent e)))
    (is (= "class1" (.-className e)))
    (is (= 2 (-> e .-childNodes .-length)))
    (is (= "<span id=\"id1\">span1</span><span id=\"id2\">span2</span>"
           (.-innerHTML e)))
    (is (= "span1" (-> e .-childNodes (aget 0) .-innerHTML)))
    (is (= "span2" (-> e .-childNodes (aget 1) .-innerHTML))))
  (let [[e _] (hipo/create [:div (for [x [1 2]] [:span {:id (str "id" x)} (str "span" x)])] )]
    (is (= "<span id=\"id1\">span1</span><span id=\"id2\">span2</span>" (.-innerHTML e)))))

(deftest attrs
  (let [[e _] (hipo/create [:a ^:attrs (merge {} {:href "http://somelink"}) "anchor"])]
    (is (-> e .-tagName (= "A")))
    (is (= "anchor" (.-textContent e)))
    (is (= "http://somelink" (.getAttribute e "href")))))

(defn my-div-with-nested-list [] [:div [:div] (list [:div "a"] [:div "b"] [:div "c"])])

(deftest nested
  ;; test html for example list form
  ;; note: if practice you can write the direct form (without the list) you should.
  (let [spans (for [i (range 2)] [:span (str "span" i)])
        end [:span.end "end"]
        [e _] (hipo/create [:div#id1.class1 (list spans end)])]
    (is (-> e .-textContent (= "span0span1end")))
    (is (-> e .-className (= "class1")))
    (is (-> e .-childNodes .-length (= 3)))
    (is (= "<span>span0</span><span>span1</span><span class=\"end\">end</span>" (.-innerHTML e)))
    (is (-> e .-childNodes (aget 0) .-innerHTML (= "span0")))
    (is (-> e .-childNodes (aget 1) .-innerHTML (= "span1")))
    (is (-> e .-childNodes (aget 2) .-innerHTML (= "end"))))
  ;; test equivalence of "direct inline" and list forms
  (let [spans (for [i (range 2)] [:span (str "span" i)])
        end   [:span.end "end"]
        [e1 _] (hipo/create [:div.class1 (list spans end)])
        [e2 _] (hipo/create [:div.class1 spans end])]
    (is (= (.-innerHTML e1) (.-innerHTML e2))))
  (let [[e _] (hipo/create (my-div-with-nested-list))]
    (is (= 4 (.. e -childNodes -length)))
    (is (= "abc" (.-textContent e)))))

(defn my-button [s] [:button s])

(deftest function
  (let [[e _] (hipo/create (my-button "label"))]
    (is (= "BUTTON" (.-tagName e)))
    (is (= "label" (.-textContent e))))
  (let [[e _] (hipo/create [:div (my-button "label") (my-button "label")])]
    (is (= "BUTTON" (.-tagName (.-firstChild e))))
    (is (= "label" (.-textContent (.-firstChild e))))))

(deftest boolean-attribute
  (let [[e1 _] (hipo/create [:div {:attr true} "some text"])
        [e2 _] (hipo/create [:div {:attr false} "some text"])
        [e3 _] (hipo/create [:div {:attr nil} "some text"])]
    (is (= "true" (.getAttribute e1 "attr")))
    (is (nil? (.getAttribute e2 "attr")))
    (is (nil? (.getAttribute e3 "attr")))))

(deftest camel-case-attribute
  (let [[el _] (hipo/create [:input {:defaultValue "default"}])]
    (is (= "default" (.getAttribute el "defaultValue")))))

(defn my-div [] [:div {:on-dragend (fn [])}])

(deftest listener
  (let [[e _] (hipo/create [:div {:on-drag (fn [])}])]
    (is (nil? (.getAttribute e "on-drag"))))
  (let [[e _] (hipo/create (my-div))]
    (is (nil? (.getAttribute e "on-dragend")))))

(defn my-nil [] [:div nil "content" nil])
(defn my-when [b o] (when b o))

(deftest nil-children
  (let [[e _] (hipo/create [:div nil "content" nil])]
    (is (= "content" (.-textContent e))))
  (let [[e _] (hipo/create [:div (my-when false "prefix") "content"])]
    (is (= "content" (.-textContent e))))
  (let [[e _] (hipo/create (my-nil))]
    (is (= "content" (.-textContent e)))))

(deftest custom-elements
  (is (exists? (.-registerElement js/document)))
  (.registerElement js/document "my-custom" #js {:prototype (js/Object.create (.-prototype js/HTMLDivElement) #js {:test #js {:get (fn[] "")}})})
  (let [[e _] (hipo/create [:my-custom "content"])]
    (is (exists? (.-test e)))
    (is (-> e .-tagName (= "MY-CUSTOM")))
    (is (= "content" (.-textContent e))))
  (let [[e _] (hipo/create [:my-non-existing-custom "content"])]
    (is (not (exists? (.-test e))))
    (is (-> e .-tagName (= "MY-NON-EXISTING-CUSTOM")))
    (is (= "content" (.-textContent e)))))

(deftest namespaces
  (is (= "http://www.w3.org/1999/xhtml" (.-namespaceURI (first (hipo/create [:p])))))
  (is (= "http://www.w3.org/2000/svg" (.-namespaceURI (first (hipo/create [:svg/circle])))))
  (is (= "http://www.w3.org/2000/svg" (.-namespaceURI (first (hipo/create [:svg/circle] {:force-interpretation? true}))))))

(deftest update-simple
  (let [hf (fn [m] [:div {:id (:id m)} (:content m)])
        [el f] (hipo/create (hf {:id "id1" :content "a"}))]
    (f (hf {:id "id2" :content "b"}))

    (is (= "b" (.-textContent el)))
    (is (= "id2" (.-id el)))))

(if (exists? js/MutationObserver)
  (deftest update-nested
    (let [c1 [:div {:class "class1" :attr1 "1"} [:span "content1"] [:span]]
          c2 [:div {:attr1 nil :attr2 nil} [:span]]
          c3 [:div]
          c4 [:div {:class "class2" :attr2 "2"} [:span] [:div "content2"]]
          [el f] (hipo/create c1)
          o (js/MutationObserver. identity)]
      (.observe o el #js {:attributes true :childList true :characterData true :subtree true})

      (is "div" (.-localName el))
      (is (= 2 (.-childElementCount el)))

      (f c1)

      (is (= 0 (count (array-seq (.takeRecords o)))))

      (f c2)

      (is (not (.hasAttribute el "class")))
      (is (not (.hasAttribute el "attr1")))
      (is (not (.hasAttribute el "attr2")))
      (is (= 1 (.-childElementCount el)))

      (let [v (array-seq (.takeRecords o))]
        (is (= 4 (count v)))
        (is (= "childList" (.-type (first v))))
        (is (= "childList" (.-type (second v))))
        (is (= "attributes" (.-type (nth v 2))))
        (is (= "attr1" (.-attributeName (nth v 2))))
        (is (= "attributes" (.-type (nth v 3))))
        (is (= "class" (.-attributeName (nth v 3)))))

      (f c3)

      (is (= 0 (.-childElementCount el)))

      (let [v (array-seq (.takeRecords o))]
        (is (= 1 (count v)))
        (is (= "childList" (.-type (first v)))))

      (f c4)

      (is "div" (.-localName el))
      (is (= "class2" (.getAttribute el "class")))
      (is (not (.hasAttribute el "attr1")))
      (is (= "2" (.getAttribute el "attr2")))
      (is (= 2 (.-childElementCount el)))
      (let [c (.-firstChild el)]
        (is (= "span" (.-localName c))))
      (let [c (.. el -firstChild -nextElementSibling)]
        (is (= "div" (.-localName c)))
        (is (= "content2" (.-textContent c))))

      (let [v (array-seq (.takeRecords o))]
        (is (= 3 (count v)))
        (is (= "childList" (.-type (first v))))
        (is (= "attributes" (.-type (second v))))
        (is (= "class" (.-attributeName (second v))))
        (is (= "attributes" (.-type (nth v 2))))
        (is (= "attr2" (.-attributeName (nth v 2)))))

      (.disconnect o))))

(defn fire-click-event
  [el]
  (let [ev (.createEvent js/document "HTMLEvents")]
    (.initEvent ev "click" true true)
    (.dispatchEvent el ev)))

(deftest update-listener
  (let [a (atom 0)
        hf (fn [b] [:div (if b {:on-click #(swap! a inc)})])
        [el f] (hipo/create (hf true))]
    (fire-click-event el)
    (f (hf false))
    (fire-click-event el)

    (is (= 1 @a))
    (f (hf true))
    (fire-click-event el)
    (is (= 2 @a))))

(deftest update-listener-as-map
  (let [a (atom 0)
        hf (fn [m] [:div ^:attrs (if m {:on-click {:name "click" :fn #(when-let [f (:fn m)] (swap! a (fn [evt] (f evt))))}})])
        [el f] (hipo/create (hf {:fn #(inc %)}))]
    (fire-click-event el)
    (is (= 1 @a))
    (f (hf nil))
    (fire-click-event el)

    (is (= 1 @a))
    (f (hf {:fn #(dec %)}))
    (fire-click-event el)
    (is (= 0 @a))))

(deftest update-keyed
  (let [hf (fn [r] [:ul (for [i r] ^{:hipo/key i} [:li {:class i} i])])
        [el f] (hipo/create (hf (range 6)))]
    (f (hf (reverse (range 6))))

    (is (= 6 (.. el -childNodes -length)))
    (is (= "5" (.. el -firstChild -textContent)))
    (is (= "5" (.. el -firstChild -className)))
    (is (= "4" (.. el -firstChild -nextSibling -textContent)))
    (is (= "3" (.. el -firstChild -nextSibling -nextSibling -textContent)))
    (is (= "2" (.. el -firstChild -nextSibling -nextSibling -nextSibling -textContent)))
    (is (= "1" (.. el -firstChild -nextSibling -nextSibling -nextSibling -nextSibling -textContent)))
    (is (= "0" (.. el -lastChild -textContent)))))

(deftest update-keyed-sparse
  (let [hf (fn [r] [:ul (for [i r] ^{:hipo/key i} [:li {:class i} i])])
        [el f] (hipo/create (hf (range 6)))]
    (f (hf (cons 7 (filter odd? (reverse (range 6))))))

    (is (= 4 (.. el -childNodes -length)))
    (is (= "7" (.. el -firstChild -textContent)))
    (is (= "7" (.. el -firstChild -className)))
    (is (= "5" (.. el -firstChild -nextSibling -textContent)))
    (is (= "3" (.. el -firstChild -nextSibling -nextSibling -textContent)))
    (is (= "1" (.. el -firstChild -nextSibling -nextSibling -nextSibling -textContent)))))

(deftest update-state
  (let [m1 {:children (range 6)}
        m2 {:children (cons 7 (filter odd? (reverse (range 6))))}
        hf (fn [m]
             [:ul (for [i (:children m)]
                    ^{:hipo/key i} [:li {:class i} i])])
        [el f] (hipo/create (hf m1))]
    (f (hf m2) m2)

    (is (= 4 (.. el -childNodes -length)))
    (is (= "7" (.. el -firstChild -textContent)))
    (is (= "7" (.. el -firstChild -className)))
    (is (= "5" (.. el -firstChild -nextSibling -textContent)))
    (is (= "3" (.. el -firstChild -nextSibling -nextSibling -textContent)))
    (is (= "1" (.. el -firstChild -nextSibling -nextSibling -nextSibling -textContent)))))

(deftype MyInterceptor []
  Interceptor
  (-intercept [_ t _ f]
    ; let update be performed but reject all others
    (if (= :update t)
      (f))))

(deftype MyOtherInterceptor []
  Interceptor
  (-intercept [_ _ _ f]
    ; let update be performed but reject all others
    (f)))

(deftest interceptor
  (let [hf (fn [m] [:div {:class (:value m)}])
        [el f] (hipo/create (hf {:value 1}))]
    (f (hf {:value 2}) {:interceptors [(MyOtherInterceptor.)]})
    (is (= "2" (.-className el)))
    (f (hf {:value 3}) {:interceptors [(MyInterceptor.)]})
    (is (= "2" (.-className el)))
    (f (hf {:value 3}) {:interceptors [(MyInterceptor.) (MyOtherInterceptor.)]})
    (is (= "2" (.-className el)))
    (f (hf {:value 4}) {:interceptors [(MyOtherInterceptor.) (MyInterceptor.)]})
    (is (= "2" (.-className el)))))
