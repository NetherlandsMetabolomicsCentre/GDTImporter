<%
/**
 * second wizard page / tab
 *
 * @author Jeroen Wesbeek
 * @since  20101206
 *
 * Revision information:
 * $Rev: 1430 $
 * $Author: work@osx.eu $
 * $Date: 2011-01-21 21:05:36 +0100 (Fri, 21 Jan 2011) $
 */
%>
<af:page>
<h1>Assign properties to columns</h1>
  <p>Below you see a preview of your imported file, please correct the automatically detected types.</p>  
  <GdtImporter:properties header="${importer_header}" datamatrix="${importer_datamatrix}"/>
</af:page>