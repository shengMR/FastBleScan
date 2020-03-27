# FastBleScan
## 版本更新信息

### v1.0.0

* 正式版提交



## 具体使用：

### 1，引入库

```

```

```

```



### 2，使用

* 开始扫描

```java
FastBleScanner.getInstance()
	.setFilterName("ScanName") // 设置过滤名称
	.setIgnoreSame(true) // 设置扫描结果不重复
	.setScanTime(3) // 设置扫描时间(单位秒)
	.setScanCallback(new FastBleScanCallback() { // 设置回调
   		@Override
      public void onStartScan() {
				// 开始扫描回调
      }

      @Override
      public void onLeScan(ScanDevice scanDevice) {
				// 扫描设备回调
      }

      @Override
      public void onStopScan() {
				// 停止扫描回调
      }

      @Override
      public void onScanFailure(int errorCode) {
				// 扫描失败回调
      }
   }) 
	.startScan(); // 开始扫描
```

* 停止扫描

```java
FastBleScanner.getInstance().stopScan();
```

