package com.amfk.starfish.sync.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class MockApiService {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    public List<Map<String, Object>> getSiteDetails(String clusterName) {
        String sql = """
            SELECT 
                pc.name AS site,
                pc.id_pbx AS cm,
                pt.name AS type,
                pnr.range_from AS lowerbound,
                pnr.range_to AS upperbound,
                cr.country_code AS prefix
            FROM amsp.pbx_number_range pnr
            JOIN amsp.pbx_cluster pc ON pc.id = pnr.id_pbx_cluster
            JOIN amsp.pbx_phonenumber_type pt ON pt.id = pnr.phone_number_type
            JOIN amsp.country cr ON cr.id = pc.id_country
            WHERE pnr.active = 1 AND pc.active = 1 AND pc.name = ?
            """;
        
        System.out.println("MockApiService: Searching for cluster name: '" + clusterName + "'");
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, clusterName);
        System.out.println("MockApiService: Found " + result.size() + " records for cluster: '" + clusterName + "'");
        
        return result;
    }
    
    public void checkAvailableClusters() {
        String sql = "SELECT DISTINCT name FROM amsp.pbx_cluster WHERE active = 1";
        List<Map<String, Object>> clusters = jdbcTemplate.queryForList(sql);
        System.out.println("=== AVAILABLE CLUSTERS IN DATABASE ===");
        for (Map<String, Object> cluster : clusters) {
            System.out.println("Cluster: " + cluster.get("name"));
        }
        System.out.println("Total clusters found: " + clusters.size());
        System.out.println("=====================================");
    }
}