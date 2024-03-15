
this.bookmap_topicrefs = [
    "abbrevlist",
    "amendments",
    "appendices",
    "appendix",
    "backmatter",
    "bibliolist",
    "bookabstract",
    "booklibrary",
    "booklist",
    "booklists",
    "chapter",
    "colophon",
    "dedication",
    "draftintro",
    "figurelist",
    "frontmatter",
    "glossarylist",
    "indexlist",
    "notices",
    "part",
    "preface",
    "tablelist",
    "toc",
    "trademarklist"
  ];
  this.supportedKeywords = [
    "topicref",
    "mapref",
    "topichead",
    "topicgroup",
    "topicset",
    ...this.bookmap_topicrefs
  ];
  this.currId = 1;


  //Main Function to be called

  //Main Function to be called
  function getMapHierarchy(parentMapPath) {
	return new Promise((resolve, reject) => {
        this.parseMap(parentMapPath).then(parsedMap => {
            const hierarchy = this.createMapHierarchyJson(parsedMap);
            resolve(hierarchy);
        });
    });
  }



  // input is the result from parseMap()
  function createMapHierarchyJson(mapData) {
      let ditamapHierarchy = []
      let item = {
        path: mapData.parentMap,
        items: [],
        title: mapData.title
      };
      ditamapHierarchy.push(item);
      _.each(mapData, child => {
        this.pushMapChild(child, item);
      });
      return ditamapHierarchy;
    }


  //Called from createMapHierarchyJson()
  function pushMapChild(mapItem, parentItem) {
    let item = {
        path: mapItem.href,
        items: [],
        title: mapItem.titleFromMap ? mapItem.title : ""
    };
    parentItem.items.push(item);
    _.each(mapItem.children, child => {
        this.pushMapChild(child, item);
    });
  }


  // input param is the path of the parent ditamap
  function parseMap(mapPath, parentMaps) {
   return new Promise((resolve, reject) => {
     if (parentMaps && parentMaps.indexOf(mapPath) != -1) {
       //Don't show the same map again
       resolve();
     } else {
       this.getDitaContent(mapPath)
         .then(data => {
           if (!data) {
             resolve();
           }
           let domElement = new DOMParser().parseFromString(data, "text/xml")
             .documentElement;
           let newParentMaps = parentMaps ? parentMaps.slice() : [];
           newParentMaps.push(mapPath);
           this.getParsedChildren(domElement, mapPath, newParentMaps).then(
             nodeArr => {
               nodeArr = nodeArr.filter(node => node != null);
               resolve(nodeArr);
             }
           );
         })
         .catch(() => {
           resolve();
         });
     }
   });
  }

  function getDitaContent(mapPath) {
    var data = {
      path: mapPath,
      operation: "getdita"
    };
    return new Promise((resolve, reject) => {
      $.post("/bin/referencelistener", data)
        .then(resMap => {
          resolve(resMap);
        })
        .fail(() => {
          reject();
        });
    });
  }

  function isTopicref(tag) {
    return tag == "topicref" || this.bookmap_topicrefs.indexOf(tag) != -1;
  }

  function parse(domElement, parentMap, parentMaps) {
    return new Promise((resolve, reject) => {
      if (domElement.nodeType != 1) {
        return resolve();
      }

      let tag = domElement.nodeName;
      if (this.supportedKeywords.indexOf(tag) == -1) return resolve();

      if (
        (this.isTopicref(tag) &&
          domElement.getAttribute("format") != null &&
          domElement.getAttribute("format") == "ditamap") ||
        tag == "mapref"
      ) {
        this.extractTitle(domElement, parentMap).done(title => {
          let obj = {
            title: title,
            elem: domElement,
            tag: tag,
            type: "map",
            parentMap: parentMap
          };
          if (!_.isEmpty(obj.title)) {
            obj.titleFromMap = true;
          }
          if (domElement.hasAttribute("href")) {
            let mapPath = this.getAbsolutePath(
              parentMap,
              domElement.getAttribute("href")
            );
            this.parseMap(mapPath, parentMaps).then(data => {
              obj["href"] = mapPath;
              obj["children"] = data;
              resolve(obj);
            });
          } else {
            obj.id = this.currId++;
            resolve(obj);
          }
        });
      } else if (this.isTopicref(tag) || tag == "topicset" || tag == "keydef") {
        let commentsCount;
        this.extractTitle(domElement, parentMap).done(title => {
          this.getParsedChildren(domElement, parentMap, parentMaps).then(
            nodeArr => {
              let obj = {
                title: title,
                tag: tag,
                type: "topic",
                elem: domElement,
                children: nodeArr.filter(node => node != null),
                commentsCount: commentsCount,
                parentMap: parentMap
              };
              if (!_.isEmpty(obj.title)) {
                obj.titleFromMap = true;
              }
              if (domElement.hasAttribute("href")) {
                let href = this.getAbsolutePath(
                  parentMap,
                  domElement.getAttribute("href")
                );
                obj["href"] = href;
                obj["link"] = '';
              } else {
                obj.id = this.currId++;
              }
              resolve(obj);
            }
          );
        });
      } else if (tag == "topichead") {
        this.extractTitle(domElement, parentMap).done(title => {
          this.getParsedChildren(domElement, parentMap, parentMaps).then(
            nodeArr => {
              let obj = {
                title: title,
                tag: tag,
                type: "topichead",
                elem: domElement,
                children: nodeArr.filter(node => node != null),
                parentMap: parentMap,
                id: this.currId++
              };
              if (!_.isEmpty(obj.title)) {
                obj.titleFromMap = true;
              }
              resolve(obj);
            }
          );
        });
      } else if (tag == "topicgroup") {
        this.getParsedChildren(domElement, parentMap, parentMaps).then(
          nodeArr => {
            let obj = {
              title: "Topic Group", // should we localize it ?
              tag: tag,
              type: "topicgroup",
              id: this.currId++,
              elem: domElement,
              children: nodeArr.filter(node => node != null),
              titleFromMap: true
            };
            resolve(obj);
          }
        );
      }
    });
  }

  function getParsedChildren(domElement, parentMap, parentMaps) {
    let childrenArr = $(domElement)
      .children()
      .toArray();
    return Promise.all(
      childrenArr.map(child => {
        return this.parse(child, parentMap, parentMaps);
      })
    );
  }

  function extractTitle(domElement, parentMap) {
    let dfd = new $.Deferred();

    if (
      domElement.getAttribute("title") &&
      domElement.getAttribute("title").length != 0
    ) {
      dfd.resolve(domElement.getAttribute("title"));
      return dfd.promise();
    }

    let topicmeta = $(domElement).children("topicmeta")[0];
    if (topicmeta) {
      let navtitle = $(topicmeta).children("navtitle")[0];
      if (navtitle != null && navtitle.textContent.length != 0) {
        dfd.resolve(navtitle.textContent);
        return dfd.promise();
      }
    }

    if (
      domElement.getAttribute("navtitle") &&
      domElement.getAttribute("navtitle").length != 0
    ) {
      dfd.resolve(domElement.getAttribute("navtitle"));
      return dfd.promise();
    }

    if (
      domElement.getAttribute("format") == "none" &&
      domElement.getAttribute("href") == "#"
    ) {
      dfd.resolve("");
      return dfd.promise();
    }

    let href = domElement.getAttribute("href");
    if (href == null) {
      dfd.resolve(domElement.nodeName);
      return dfd.promise();
    }

    if (domElement.fm_title != null) {
      dfd.resolve(domElement.fm_title);
      return dfd.promise();
    }

    domElement.fm_title = decodeURIComponent(
      href.substring(href.lastIndexOf("/") + 1).split(".")[0]
    );
    dfd.resolve("");
    return dfd.promise();
  }

  function getAbsolutePath(rootPath, relPath) {
    if (relPath.startsWith("/")) return relPath;

    let res = rootPath.substring(0, rootPath.lastIndexOf("/"));
    relPath = relPath.split("/");
    for (let i = 0; i < relPath.length; i++) {
      if (relPath[i] == ".") continue;
      if (relPath[i] == "..") res = res.substring(0, res.lastIndexOf("/"));
      else res = res + "/" + relPath[i];
    }
    return res;
  }
