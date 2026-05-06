package cn.edu.ccst.communitysocialmain.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 省市区数据缓存服务
 * 应用启动时从腾讯地图API获取数据并缓存到Redis
 */
@Slf4j
@Service
public class RegionCacheService implements CommandLineRunner {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private cn.edu.ccst.communitysocialmain.utils.RedisUtil redisUtil;

    // 腾讯地图 API Key
    private static final String TENCENT_MAP_KEY = "2OBBZ-PJALT-SR2X7-LYJRP-ICYGV-22FPI";
    
    // Redis Key 前缀
    private static final String REDIS_KEY_PROVINCES = "region:provinces";
    private static final String REDIS_KEY_CITIES = "region:cities";
    private static final String REDIS_KEY_DISTRICTS = "region:districts";
    
    // 缓存过期时间（7天）
    private static final long CACHE_EXPIRE_DAYS = 7;

    /**
     * 应用启动时执行，加载省市区数据到Redis
     */
    @Override
    public void run(String... args) throws Exception {
        log.info("=== 开始初始化省市区数据缓存 ===");
        initRegionCache();
        log.info("=== 省市区数据缓存初始化完成 ===");
    }

    /**
     * 初始化省市区数据缓存
     */
    public void initRegionCache() {
        try {
            // 检查Redis中是否已有数据
            if (redisUtil.hasKey(REDIS_KEY_PROVINCES) && 
                redisUtil.hasKey(REDIS_KEY_CITIES) && 
                redisUtil.hasKey(REDIS_KEY_DISTRICTS)) {
                log.info("Redis中已存在省市区数据，跳过初始化");
                return;
            }

            log.info("开始从腾讯地图API获取省市区数据...");

            // 1. 获取省份列表
            List<Map<String, Object>> provinces = fetchProvinces();
            if (provinces == null || provinces.isEmpty()) {
                log.error("获取省份数据失败");
                return;
            }
            log.info("获取到{}个省份", provinces.size());

            // 2. 遍历省份获取城市和区县
            List<Map<String, Object>> allCities = new ArrayList<>();
            List<Map<String, Object>> allDistricts = new ArrayList<>();
            
            // API调用计数器（用于速率限制）
            int apiCallCount = 0;
            long lastResetTime = System.currentTimeMillis();

            for (Map<String, Object> province : provinces) {
                String provinceCode = String.valueOf(province.get("code"));
                String provinceName = String.valueOf(province.get("name"));

                // 获取该省份的城市
                List<Map<String, Object>> cities = fetchCitiesByProvince(provinceCode);
                apiCallCount++;
                
                if (cities != null && !cities.isEmpty()) {
                    log.debug("省份 {} ({}) 有 {} 个城市", provinceName, provinceCode, cities.size());
                    
                    for (Map<String, Object> city : cities) {
                        String cityCode = String.valueOf(city.get("code"));
                        
                        // 添加城市到列表
                        Map<String, Object> cityData = new HashMap<>();
                        cityData.put("code", cityCode);
                        cityData.put("name", city.get("name"));
                        cityData.put("province", provinceCode);
                        allCities.add(cityData);

                        // 获取该城市的区县
                        List<Map<String, Object>> districts = fetchDistrictsByCity(cityCode);
                        apiCallCount++;
                        
                        if (districts != null && !districts.isEmpty()) {
                            for (Map<String, Object> district : districts) {
                                Map<String, Object> districtData = new HashMap<>();
                                districtData.put("code", district.get("code"));
                                districtData.put("name", district.get("name"));
                                districtData.put("city", cityCode);
                                allDistricts.add(districtData);
                            }
                        }
                        
                        // 速率限制：每3次API调用等待1秒
                        if (apiCallCount % 3 == 0) {
                            try {
                                Thread.sleep(800); // 等待1秒
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                log.warn("线程被中断");
                            }
                        }
                    }
                }
                
                // 每个省份处理后也检查一下速率
                if (apiCallCount % 4 == 0) {
                    try {
                        Thread.sleep(800);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("线程被中断");
                    }
                }
            }

            log.info("获取到{}个城市，{}个区县", allCities.size(), allDistricts.size());

            // 3. 存储到Redis
            redisUtil.set(REDIS_KEY_PROVINCES, provinces, CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
            redisUtil.set(REDIS_KEY_CITIES, allCities, CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
            redisUtil.set(REDIS_KEY_DISTRICTS, allDistricts, CACHE_EXPIRE_DAYS, TimeUnit.DAYS);

            log.info("省市区数据已成功缓存到Redis，有效期{}天", CACHE_EXPIRE_DAYS);

        } catch (Exception e) {
            log.error("初始化省市区数据缓存失败", e);
        }
    }

    /**
     * 从腾讯地图API获取省份列表
     */
    private List<Map<String, Object>> fetchProvinces() {
        try {
            String url = "https://apis.map.qq.com/ws/district/v1/list?key=" + TENCENT_MAP_KEY + "&output=json";
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null || !"0".equals(String.valueOf(response.get("status")))) {
                log.error("获取省份数据失败：{}", response);
                return null;
            }

            List<List<Map<String, Object>>> result = (List<List<Map<String, Object>>>) response.get("result");
            if (result == null || result.isEmpty()) {
                return null;
            }

            List<Map<String, Object>> provinces = result.get(0);
            List<Map<String, Object>> formattedProvinces = new ArrayList<>();

            for (Map<String, Object> province : provinces) {
                Map<String, Object> provinceData = new HashMap<>();
                provinceData.put("code", province.get("id"));
                provinceData.put("name", province.get("name"));
                formattedProvinces.add(provinceData);
            }

            return formattedProvinces;

        } catch (Exception e) {
            log.error("获取省份列表异常", e);
            return null;
        }
    }

    /**
     * 根据省份代码获取城市列表
     */
    private List<Map<String, Object>> fetchCitiesByProvince(String provinceCode) {
        try {
            // 判断是否为直辖市
            boolean isDirectCity = "110000".equals(provinceCode) ||  // 北京
                                  "120000".equals(provinceCode) ||  // 天津
                                  "310000".equals(provinceCode) ||  // 上海
                                  "500000".equals(provinceCode);    // 重庆

            String url = "https://apis.map.qq.com/ws/district/v1/getchildren?key=" + TENCENT_MAP_KEY + 
                        "&id=" + provinceCode + "&output=json";
            
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null || !"0".equals(String.valueOf(response.get("status")))) {
                log.warn("获取省份 {} 的城市数据失败", provinceCode);
                return new ArrayList<>();
            }

            List<List<Map<String, Object>>> result = (List<List<Map<String, Object>>>) response.get("result");
            if (result == null || result.isEmpty() || result.get(0) == null) {
                return new ArrayList<>();
            }

            List<Map<String, Object>> cities = result.get(0);
            List<Map<String, Object>> formattedCities = new ArrayList<>();

            if (isDirectCity) {
                // 直辖市特殊处理：返回"市辖区"和"市辖县"
                List<Map<String, Object>> quList = new ArrayList<>();
                List<Map<String, Object>> xianList = new ArrayList<>();

                for (Map<String, Object> district : cities) {
                    String districtName = String.valueOf(district.get("fullname"));
                    Map<String, Object> districtData = new HashMap<>();
                    districtData.put("code", district.get("id"));
                    districtData.put("name", districtName);
                    districtData.put("province", provinceCode);

                    if (districtName.endsWith("区")) {
                        quList.add(districtData);
                    } else if (districtName.endsWith("县")) {
                        xianList.add(districtData);
                    }
                }

                // 添加"市辖区"选项
                if (!quList.isEmpty()) {
                    Map<String, Object> quOption = new HashMap<>();
                    quOption.put("code", provinceCode + "_QU");
                    quOption.put("name", "市辖区");
                    quOption.put("province", provinceCode);
                    quOption.put("_type", "QU");
                    quOption.put("_data", quList);
                    formattedCities.add(quOption);
                }

                // 添加"市辖县"选项
                if (!xianList.isEmpty()) {
                    Map<String, Object> xianOption = new HashMap<>();
                    xianOption.put("code", provinceCode + "_XIAN");
                    xianOption.put("name", "市辖县");
                    xianOption.put("province", provinceCode);
                    xianOption.put("_type", "XIAN");
                    xianOption.put("_data", xianList);
                    formattedCities.add(xianOption);
                }
            } else {
                // 普通省份
                for (Map<String, Object> city : cities) {
                    Map<String, Object> cityData = new HashMap<>();
                    cityData.put("code", city.get("id"));
                    cityData.put("name", city.get("fullname"));
                    cityData.put("province", provinceCode);
                    formattedCities.add(cityData);
                }
            }

            return formattedCities;

        } catch (Exception e) {
            log.error("获取省份 {} 的城市列表异常", provinceCode, e);
            return new ArrayList<>();
        }
    }

    /**
     * 根据城市代码获取区县列表
     */
    private List<Map<String, Object>> fetchDistrictsByCity(String cityCode) {
        try {
            // 判断是否是直辖市的"市辖区"或"市辖县"选项
            if (cityCode.endsWith("_QU") || cityCode.endsWith("_XIAN")) {
                String provinceCode = cityCode.substring(0, 6);
                boolean isQu = cityCode.endsWith("_QU");

                String url = "https://apis.map.qq.com/ws/district/v1/getchildren?key=" + TENCENT_MAP_KEY + 
                            "&id=" + provinceCode + "&output=json";
                
                Map<String, Object> response = restTemplate.getForObject(url, Map.class);

                if (response == null || !"0".equals(String.valueOf(response.get("status")))) {
                    return new ArrayList<>();
                }

                List<List<Map<String, Object>>> result = (List<List<Map<String, Object>>>) response.get("result");
                if (result == null || result.isEmpty() || result.get(0) == null) {
                    return new ArrayList<>();
                }

                List<Map<String, Object>> allDistricts = result.get(0);
                List<Map<String, Object>> filteredDistricts = new ArrayList<>();

                for (Map<String, Object> district : allDistricts) {
                    String districtName = String.valueOf(district.get("fullname"));
                    boolean matchesType = isQu ? districtName.endsWith("区") : districtName.endsWith("县");

                    if (matchesType) {
                        Map<String, Object> districtData = new HashMap<>();
                        districtData.put("code", district.get("id"));
                        districtData.put("name", districtName);
                        districtData.put("city", cityCode);
                        filteredDistricts.add(districtData);
                    }
                }

                return filteredDistricts;
            }

            // 普通城市的区县
            String url = "https://apis.map.qq.com/ws/district/v1/getchildren?key=" + TENCENT_MAP_KEY + 
                        "&id=" + cityCode + "&output=json";
            
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null || !"0".equals(String.valueOf(response.get("status")))) {
                return new ArrayList<>();
            }

            List<List<Map<String, Object>>> result = (List<List<Map<String, Object>>>) response.get("result");
            if (result == null || result.isEmpty() || result.get(0) == null) {
                return new ArrayList<>();
            }

            List<Map<String, Object>> districts = result.get(0);
            List<Map<String, Object>> formattedDistricts = new ArrayList<>();

            for (Map<String, Object> district : districts) {
                Map<String, Object> districtData = new HashMap<>();
                districtData.put("code", String.valueOf(district.get("id")));
                districtData.put("name", String.valueOf(district.get("fullname")));
                districtData.put("city", cityCode);
                formattedDistricts.add(districtData);
            }

            return formattedDistricts;

        } catch (Exception e) {
            log.error("获取城市 {} 的区县列表异常", cityCode, e);
            return new ArrayList<>();
        }
    }

    /**
     * 从 Redis 获取省份列表
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getProvinces() {
        try {
            Object data = redisUtil.get(REDIS_KEY_PROVINCES);
            if (data instanceof List) {
                List<Map<String, Object>> provinces = (List<Map<String, Object>>) data;
                log.debug("从 Redis 获取到 {} 个省份", provinces.size());
                return provinces;
            } else {
                log.warn("Redis 中省份数据格式不正确，类型: {}", data != null ? data.getClass().getName() : "null");
            }
        } catch (Exception e) {
            log.error("从 Redis 获取省份列表失败", e);
        }
        return new ArrayList<>();
    }

    /**
     * 从Redis获取城市列表
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getCities() {
        try {
            Object data = redisUtil.get(REDIS_KEY_CITIES);
            if (data instanceof List) {
                return (List<Map<String, Object>>) data;
            }
        } catch (Exception e) {
            log.error("从Redis获取城市列表失败", e);
        }
        return new ArrayList<>();
    }

    /**
     * 从Redis获取区县列表
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getDistricts() {
        try {
            Object data = redisUtil.get(REDIS_KEY_DISTRICTS);
            if (data instanceof List) {
                return (List<Map<String, Object>>) data;
            }
        } catch (Exception e) {
            log.error("从Redis获取区县列表失败", e);
        }
        return new ArrayList<>();
    }

    /**
     * 根据省份代码过滤城市
     */
    public List<Map<String, Object>> getCitiesByProvinceCode(String provinceCode) {
        List<Map<String, Object>> allCities = getCities();
        List<Map<String, Object>> filtered = new ArrayList<>();
        
        for (Map<String, Object> city : allCities) {
            if (provinceCode.equals(city.get("province"))) {
                filtered.add(city);
            }
        }
        
        return filtered;
    }

    /**
     * 根据城市代码过滤区县
     */
    public List<Map<String, Object>> getDistrictsByCityCode(String cityCode) {
        List<Map<String, Object>> allDistricts = getDistricts();
        List<Map<String, Object>> filtered = new ArrayList<>();
        
        for (Map<String, Object> district : allDistricts) {
            if (cityCode.equals(district.get("city"))) {
                filtered.add(district);
            }
        }
        
        return filtered;
    }

    /**
     * 获取完整的省市区数据（用于后台管理端一次性加载）
     */
    public Map<String, Object> getAllRegions() {
        Map<String, Object> result = new HashMap<>();
        result.put("provinces", getProvinces());
        result.put("cities", getCities());
        result.put("districts", getDistricts());
        return result;
    }

    /**
     * 手动刷新缓存（管理员接口调用）
     */
    public void refreshCache() {
        log.info("手动刷新省市区数据缓存...");
        // 删除旧缓存
        redisUtil.delete(REDIS_KEY_PROVINCES);
        redisUtil.delete(REDIS_KEY_CITIES);
        redisUtil.delete(REDIS_KEY_DISTRICTS);
        
        // 重新加载
        initRegionCache();
    }
}
