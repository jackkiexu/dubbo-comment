ReferenceBean.getObject()
  --> ReferenceConfig.get()
    --> ReferenceConfig.init()
      --> createProxy(map)
        --> refprotocol.refer(interfaceClass, urls.get(0))