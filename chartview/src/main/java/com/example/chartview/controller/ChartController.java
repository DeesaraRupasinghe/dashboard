package com.example.chartview.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/charts")
public class ChartController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/test")
    public String testEndpoint() {
        return "API working!";
    }
    @GetMapping("/all-configs")
    public List<Map<String, Object>> getAllChartConfigs() {
        String sql = "SELECT id, chart_name, chart_type FROM chart_config ORDER BY id";
        return jdbcTemplate.queryForList(sql);
    }
    

    @GetMapping("/multi-line")
    public Map<String, Object> getMultiTableChart(@RequestParam List<Integer> chartIds) {
        List<Map<String, Object>> datasets = new ArrayList<>();
        String xAxisCommon = "";
        List<String> labels = new ArrayList<>();

        for (int chartId : chartIds) {
            try {
                String configSql = "SELECT * FROM chart_config WHERE id = ?";
                Map<String, Object> config = jdbcTemplate.queryForMap(configSql, chartId);

                String tableName = config.get("table_name").toString();
                String xAxis = config.get("x_axis").toString();
                String yAxis = config.get("y_axis").toString();
                String chartName = config.get("chart_name").toString();

                if (!isValidColumnName(xAxis) || !isValidColumnName(yAxis) || !isValidTableName(tableName)) {
                    throw new IllegalArgumentException("Invalid table or column name");
                }

                if (xAxisCommon.isEmpty()) xAxisCommon = xAxis;

                String dataSql = String.format("SELECT %s, %s FROM %s ORDER BY %s", xAxis, yAxis, tableName, xAxis);
                List<Map<String, Object>> data = jdbcTemplate.queryForList(dataSql);

                List<Object> xValues = new ArrayList<>();
                List<Number> yValues = new ArrayList<>();

                for (Map<String, Object> row : data) {
                    xValues.add(row.get(xAxis));
                    yValues.add(Double.parseDouble(row.get(yAxis).toString()));
                }

                datasets.add(Map.of(
                        "label", chartName,
                        "data", yValues
                ));

                if (labels.isEmpty()) {
                    for (Object x : xValues) {
                        labels.add(x.toString());
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to fetch chart data for ID " + chartId + ": " + e.getMessage());
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("labels", labels);
        response.put("datasets", datasets);
        return response;
    }

    @GetMapping("/combined-columns")
    public Map<String, Object> getCombinedChart() {
        Map<String, Object> response = new HashMap<>();
        try {
            String xSql = "SELECT order_date FROM orders ORDER BY order_date";
            String ySql = "SELECT amount FROM sales ORDER BY date";

            List<Map<String, Object>> xData = jdbcTemplate.queryForList(xSql);
            List<Map<String, Object>> yData = jdbcTemplate.queryForList(ySql);

            List<String> labels = new ArrayList<>();
            List<Number> values = new ArrayList<>();

            int count = Math.min(xData.size(), yData.size());

            for (int i = 0; i < count; i++) {
                labels.add(xData.get(i).get("order_date").toString());
                values.add(Double.parseDouble(yData.get(i).get("amount").toString()));
            }

            response.put("labels", labels);
            response.put("data", values);
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Failed to build combined chart: " + e.getMessage());
        }
    }

    @GetMapping("/{chartId}")
    public Map<String, Object> getChartData(
            @PathVariable int chartId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            String configSql = "SELECT * FROM chart_config WHERE id = ?";
            Map<String, Object> config = jdbcTemplate.queryForMap(configSql, chartId);

            String tableName = config.get("table_name").toString();
            String xAxis = config.get("x_axis").toString();
            String yAxis = config.get("y_axis").toString();
            String chartType = config.get("chart_type").toString();

            if (!isValidColumnName(xAxis) || !isValidColumnName(yAxis) || !isValidTableName(tableName)) {
                throw new IllegalArgumentException("Invalid table or column name");
            }

            StringBuilder dataSql = new StringBuilder(String.format("SELECT %s, %s FROM %s WHERE 1=1", xAxis, yAxis, tableName));
            List<Object> params = new ArrayList<>();

            if (startDate != null) {
                dataSql.append(" AND ").append(xAxis).append(" >= ?");
                params.add(startDate);
            }
            if (endDate != null) {
                dataSql.append(" AND ").append(xAxis).append(" <= ?");
                params.add(endDate);
            }

            dataSql.append(" ORDER BY ").append(xAxis);

            List<Map<String, Object>> data = jdbcTemplate.queryForList(dataSql.toString(), params.toArray());

            Map<String, Object> response = new HashMap<>();
            response.put("chartName", config.get("chart_name"));
            response.put("chartType", chartType);
            response.put("xAxis", xAxis);
            response.put("yAxis", yAxis);
            response.put("data", data);

            return response;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch chart data: " + e.getMessage());
        }
    }

    private boolean isValidColumnName(String name) {
        return name != null && name.matches("^[a-zA-Z0-9_]+$");
    }

    private boolean isValidTableName(String name) {
        return name != null && name.matches("^[a-zA-Z0-9_]+$");
    }
}
