package com.example;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

public class TestCreateEquipmentReport {
    private Playwright playwright;
    private Browser browser;

    @BeforeEach
    public void setUp() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
    }

    @AfterEach
    public void tearDown() {
        browser.close();
        playwright.close();
    }

    @Test
    public void testCreateEquipmentReport() {
        BrowserContext context = browser.newContext();
        Page page = context.newPage();
        
        login(page, "candidate.ELFON", "gauge13");

        navigateToStatusReports(page);

        // Create a non-scheduled report
        createReport(page, "HardwareTest", "Equipment", "No");
        verifyReportExists(page, "HardwareTest", false);

        // Create a scheduled report
        createReport(page, "HardwareTestScheduled", "Equipment", "Minutely", "Every 5 minutes", "10");
        verifyReportExists(page, "HardwareTestScheduled", true);

        context.close();
    }

    private void login(Page page, String username, String password) {
        page.navigate("https://octopus.evs.online/app/login");

        page.fill("#username", username);
        page.fill("#password", password);
        page.waitForNavigation(() -> page.click("#sign-in"));
    }

    private void navigateToStatusReports(Page page) {
        page.waitForSelector("a[href='/app/incidents/manage-exports']");
        page.waitForNavigation(() -> page.click("a[href='/app/incidents/manage-exports']"));
        page.waitForSelector("a[href='/app/status/reports']");
        page.waitForNavigation(() -> page.click("a[href='/app/status/reports']"));
    }

    private void createReport(Page page, String reportName, String reportType, String scheduleInterval, String... additionalOptions) {
        page.waitForSelector(".btn.new-report");
        page.click(".btn.new-report");
        page.waitForSelector("#createReport");

        page.click("button[data-id='inputType']");
        page.click("div.form-group.report-types .dropdown-menu li a:has-text('" + reportType + "')");
        String selectedOption = page.innerText("button[data-id='inputType'] .filter-option.pull-left");
        Assertions.assertEquals(reportType, selectedOption.trim());

        page.fill("#inputName", reportName);
        Assertions.assertEquals(reportName, page.inputValue("#inputName"));

        page.click("button[data-id='selectScheduleInterval']");
        page.click(".dropdown-menu li a:has-text('" + scheduleInterval + "')");
        String selectedSchedule = page.innerText("button[data-id='selectScheduleInterval'] .filter-option.pull-left");
        Assertions.assertEquals(scheduleInterval, selectedSchedule.trim());

        if (scheduleInterval.equals("Minutely")) {
            page.fill("#inputScheduleMaxOldReports", additionalOptions[1]);
        }

        page.click("button.btn.btn-danger.modal-confirm:has-text('Create')");
    }

    private void verifyReportExists(Page page, String reportName, boolean isScheduled) {
        if (isScheduled) {
            page.waitForSelector("#schedulesRegion table.table-hover.table-fixed");
            boolean isInScheduledReports = page.isVisible("#schedulesRegion table.table-hover.table-fixed td:has-text('" + reportName + "')");
            Assertions.assertTrue(isInScheduledReports, "Report '" + reportName + "' should be found in the scheduled reports table.");
        } else {
            page.waitForSelector("div.list-region.js-finished-loading table.table-hover.table-fixed");
            boolean isInReports = page.isVisible("div.list-region.js-finished-loading table.table-hover.table-fixed td:has-text('" + reportName + "')");
            Assertions.assertTrue(isInReports, "Report '" + reportName + "' should be found in the reports table.");
        }
    }
}

