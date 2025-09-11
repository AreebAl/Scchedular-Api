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
    
    public List<Map<String, Object>> getSiteDetails(String siteName) {
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
        
        return jdbcTemplate.queryForList(sql, siteName);
    }
}