package edu.ncsu.csc.itrust.unit.action;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import edu.ncsu.csc.itrust.beans.DiagnosisBean;
import edu.ncsu.csc.itrust.beans.DiagnosisStatisticsBean;
import edu.ncsu.csc.itrust.exception.DBException;
import edu.ncsu.csc.itrust.exception.FormValidationException;
import edu.ncsu.csc.itrust.unit.datagenerators.TestDataGenerator;
import edu.ncsu.csc.itrust.unit.testutils.TestDAOFactory;
import edu.ncsu.csc.itrust.action.*;
import junit.framework.TestCase;

@SuppressWarnings("unused")
public class ViewDiagnosisStatisticsActionTest extends TestCase {

	private TestDataGenerator gen = new TestDataGenerator();
	private ViewDiagnosisStatisticsAction action;
	
	private int thisYear = Calendar.getInstance().get(Calendar.YEAR);

	
	protected void setUp() throws Exception {
		gen.clearAllTables();
		gen.standardData();
		gen.loadSQLFile("patient_hcp_visits");
		gen.loadSQLFile("hcp_diagnosis_data");
		
		action = new ViewDiagnosisStatisticsAction(TestDAOFactory.getTestInstance());
	}
	
	public void testGetDiagnosisCodes() throws Exception {
		List<DiagnosisBean> db = action.getDiagnosisCodes();
		assertEquals(19, db.size());
	}
	
	public void testGetDiagnosisStatisticsInRangeValid() throws Exception {
		DiagnosisStatisticsBean dsBean = action.getDiagnosisStatisticsInRange("06/28/2011", "09/28/2011", "487.00", "27606-1234");
		assertEquals(2, dsBean.getZipStats());
		assertEquals(5, dsBean.getRegionStats());
	}
	
	public void testGetDiagnosisStatisticsInRangeValidNull() throws Exception {
		DiagnosisStatisticsBean dsBean = action.getDiagnosisStatisticsInRange(null, "09/28/2011", "487.00", "27606");
		assertEquals(null, dsBean);
		dsBean = action.getDiagnosisStatisticsInRange("06/28/2011", null, "487.00", "27606");
		assertEquals(null, dsBean);
		dsBean = action.getDiagnosisStatisticsInRange("06/28/2011", "09/28/2011", null, "27606");
		assertEquals(null, dsBean);
	}
	
	public void testGetDiagnosisStatisticsInRangeInvalidDate() throws Exception {
		try {
			action.getDiagnosisStatisticsInRange("06-28/2011", "09/28/2011", "487.00", "27606");
			fail("Should have failed but didn't");
		} catch (FormValidationException e) {
			assertEquals(1, e.getErrorList().size());
			assertEquals("Enter dates in MM/dd/yyyy", e.getErrorList().get(0));
		}
	}
	
	
	public void testGetDiagnosisStatisticsInRangeReversedDates() throws Exception {
		try {
			action.getDiagnosisStatisticsInRange("09/28/2011", "06/28/2011", "487.00", "27606");
			fail("Should have failed but didn't");
		} catch (FormValidationException e) {
			assertEquals(1, e.getErrorList().size());
			assertEquals("Start date must be before end date!", e.getErrorList().get(0));
		}
	}
	
	public void testGetDiagnosisStatisticsInRangeInvalidZip() throws Exception {
		try {
			action.getDiagnosisStatisticsInRange("06/28/2011", "09/28/2011", "487.00", "2766");
			fail("Should have failed but didn't");
		} catch (FormValidationException e) {
			assertEquals(1, e.getErrorList().size());
			assertEquals("Zip Code must be 5 digits!", e.getErrorList().get(0));
		}
	}
	
	public void testGetDiagnosisStatisticsInRangeInvalidICDCode() throws Exception {
		try {
			action.getDiagnosisStatisticsInRange("06/28/2011", "09/28/2011", "11114.00", "27606");
			fail("Should have failed but didn't");
		} catch (FormValidationException e) {
			assertEquals(1, e.getErrorList().size());
			assertEquals("ICDCode must be valid diagnosis!", e.getErrorList().get(0));
		}
	}

	public void testGetDiagnosisStatisticsByWeek() throws Exception{
	    ArrayList<DiagnosisStatisticsBean> d = action.getDiagnosisStatisticsByWeek("09/28/2011", "487.00", "27606");;
	    assertEquals(8,d.size());
		assertEquals(new SimpleDateFormat("MM/dd/yyyy").parse("08/03/2011"), d.get(0).getStartDate());
        assertEquals(new SimpleDateFormat("MM/dd/yyyy").parse("09/21/2011"), d.get(d.size()-1).getStartDate());
    }

	public void testGetDiagnosisStatisticsByWeekValidNull() throws Exception {
		List<DiagnosisStatisticsBean> dsBeans = action.getDiagnosisStatisticsByWeek(null, "487.00", "27606");
		assertNull(dsBeans);
		dsBeans = action.getDiagnosisStatisticsByWeek("09/28/2011", null, "27606");
		assertNull(dsBeans);
	}

	public void testGetDiagnosisStatisticsByWeekInvalidDate() throws Exception {
		try {
			action.getDiagnosisStatisticsByWeek("09-28/2011", "487.00", "27606");
			fail("Should have failed but didn't");
		} catch (FormValidationException e) {
			assertEquals(1, e.getErrorList().size());
			assertEquals("Enter dates in MM/dd/yyyy", e.getErrorList().get(0));
		}
	}

	public void testIsMalariaEpidemic() throws Exception {
		gen.loadSQLFile("malariaEpidemic");
		assertTrue(action.isMalariaEpidemic("11/02/" + thisYear, "27606", "110"));
		assertFalse(action.isMalariaEpidemic("11/16/" + thisYear, "27606", "110"));
	}
	
	public void testIsFluEpidemic() throws Exception {
		gen.loadSQLFile("influenzaEpidemic");
		assertTrue(action.isFluEpidemic("11/02/" + thisYear, "27606"));
		assertFalse(action.isFluEpidemic("11/16/" + thisYear, "27606"));
	}
	
	public void testGetEpidemicStatisticsInvalidThreshold(){
		try{
			ArrayList<DiagnosisStatisticsBean> dsList = action.getEpidemicStatistics("11/02/" + thisYear, "84.50", "27606", "");
			fail("FormValidationException should have been thrown.");
		}catch(FormValidationException e){
			//This should be thrown
		} catch (DBException e) {
			fail("DB Exception thrown");
		}
	}
	
	public void testGetEpidemicStatistics() {
		try {
			ArrayList<DiagnosisStatisticsBean> dsList = 
					action.getEpidemicStatistics("11/02/2012", "487.00", 
							"00601", "5");
			assertEquals(2, dsList.size());
		} catch (FormValidationException e) {
			fail("FormValidationException");
		} catch (DBException d) {
			fail("DBException thrown");
		}
	}
}
