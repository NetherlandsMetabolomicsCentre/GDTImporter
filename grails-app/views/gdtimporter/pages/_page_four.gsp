<%
/**
 * fourth wizard page / tab
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
  <h1>Import wizard imported data postview</h1>
    <p>A total of ${importer_importeddata.size()} rows were imported, below an overview of the rows is shown.</span>
    <importer:postview datamatrix="${importer_importeddata}"/>
</af:page>