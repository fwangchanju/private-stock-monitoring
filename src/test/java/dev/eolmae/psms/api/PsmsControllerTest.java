package dev.eolmae.psms.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class PsmsControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void dashboardEndpointReturnsSeededDashboardData() throws Exception {
		mockMvc.perform(get("/api/dashboard"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.snapshotTime").value("2026-03-13T10:00:00"))
			.andExpect(jsonPath("$.marketOverviews[0].marketType").value("KOSDAQ"))
			.andExpect(jsonPath("$.marketOverviews[1].marketType").value("KOSPI"))
			.andExpect(jsonPath("$.watchStocks[0].stockCode").value("005930"))
			.andExpect(jsonPath("$.notificationSetting.reminderTime").value("10:00:00"));
	}

	@Test
	void programTradingHistoryEndpointReturnsSeries() throws Exception {
		mockMvc.perform(get("/api/stocks/005930/program-trading")
				.param("from", "2026-03-13T09:45:00")
				.param("to", "2026-03-13T10:05:00"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.stockCode").value("005930"))
			.andExpect(jsonPath("$.items.length()").value(2));
	}
}
