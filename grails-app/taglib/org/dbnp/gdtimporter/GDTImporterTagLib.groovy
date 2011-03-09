/**
 *  GDTImporter, a plugin for importing data into Grails Domain Templates
 *  Copyright (C) 2011 Tjeerd Abma
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  $Author$
 *  $Rev$
 *  $Date$
 */

package org.dbnp.gdtimporter

import org.dbnp.gdt.*
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.DataFormatter

/**
 * The GDTImporter tag library contains easy tags for displaying and
 * working with imported data
 */

class GDTImporterTagLib {
    static namespace = 'gdtimporter'
	def ImporterService
	def GdtService

    /**
	 * @param header string array containing header
	 * @param datamatrix two dimensional array containing actual data
	 * @return preview of the data with the ability to adjust the datatypes
	 */
	def preview = { attrs ->

		def header = attrs['header']
		def datamatrix = attrs['datamatrix']

		out << render(template: "common/preview", model: [header: header, datamatrix: datamatrix])
	}

	/**
	 * @param datamatrix two dimensional array containing entities with read values
	 * @return postview of the imported data
	 */
	def postview = { attrs ->
		def datamatrix = attrs['datamatrix']

		out << render(template: "common/postview", model: [datamatrix: datamatrix])
	}

	def entity = { attrs ->
		out << entities[attrs['index']].name
	}

	def datapreview = { attrs ->
		def datamatrix = attrs['datamatrix']
		out << render(template: "common/datapreview", model: [datamatrix: datamatrix])
	}

	/**
	 * Show missing properties
	 */
	def missingProperties = { attrs ->
		def datamatrix = attrs['datamatrix']
		def failedcells = attrs['failedcells']
		out << render(template: "common/missingproperties", model: [datamatrix: datamatrix, failedcells: failedcells])
	}

	/**
	 * Show failed cells
	 */
	def failedCells = { attrs ->
		def failedcells = attrs['failedcells']
		out << render(template: "common/failedcells", model: [failedcells: failedcells])
	}

	/**
	 * @param entities array containing selected entities
	 * @param header array containing mappingcolumn objects
	 * @param allfieldtypes if set, show all fields
	 */
	def properties = { attrs ->
		def header = attrs['header']
		def entities = attrs['entities']

		out << render(template: "common/properties_horizontal", model: [header:header])
	}

	/**
	 * Possibly this will later on return an AJAX-like autocompletion chooser for the fields?
	 *
	 * @param name name for the property chooser element
	 * @param importtemplate_id template identifier where fields are retrieved from
	 * @param matchvalue value which will be looked up via fuzzy matching against the list of options and will be selected
     * @param selected value to be selected by default
	 * @param MappingColumn object containing all required information
     * @param fuzzymatching boolean true if fuzzy matching should be used, otherwise false
	 * @param allfieldtypes boolean true if all templatefields should be listed, otherwise only show filtered templatefields
	 * @return chooser object
	 * */
	def propertyChooser = { attrs ->
		// TODO: this should be changed to retrieving fields per entity instead of from one template
		//	 and session variables should not be used inside the service, migrate to controller

		def t = Template.get(attrs['template_id'])
		def mc = attrs['mappingcolumn']
		def allfieldtypes = attrs['allfieldtypes']
        def matchvalue = (attrs['fuzzymatching']=="true") ? attrs['matchvalue'] : ""
        def selected = (attrs['selected']) ? attrs['selected'] : ""
		def fuzzyTreshold = attrs[ 'treshold' ] && attrs[ 'treshold' ].toString().isNumber() ? Float.valueOf( attrs[ 'treshold' ] ) : 0.1;
        def returnmatchonly = attrs['returnmatchonly']

        def domainfields = mc.entityclass.giveDomainFields().findAll { it.type == mc.templatefieldtype }
		domainfields = domainfields.findAll { it.preferredIdentifier != mc.identifier}

		//def templatefields = (allfieldtypes=="true") ? t.fields : t.fields.findAll { it.type == mc.templatefieldtype }
		/*def templatefields = (allfieldtypes == "true") ?
			t.fields + mc.entity.giveDomainFields() :
			t.fields.findAll { it.type == mc.templatefieldtype } + domainfields*/
        def templatefields = t.fields + mc.entityclass.giveDomainFields()

		// map identifier to preferred column
		def prefcolumn = mc.entityclass.giveDomainFields().findAll { it.preferredIdentifier == true }

		/*(mc.identifier) ? out << createPropertySelect(attrs['name'], prefcolumn, matchvalue, mc.index) :
			out << createPropertySelect(attrs['name'], templatefields, matchvalue, mc.index)*/

        //  Just return the matched value only
        if (returnmatchonly)
            out << importerService.mostSimilar(matchvalue, templatefields, fuzzyTreshold)
        else // Return a selectbox
            out << createPropertySelect(attrs['name'], templatefields, matchvalue, selected, mc.index, fuzzyTreshold)

	}

	/**
	 * Create the property chooser select element
	 *
	 * @param name name of the HTML select object
	 * @param options list of options (fields) to be used
	 * @param matchvalue value which will be looked up via fuzzy matching against the list of options and will be selected
	 * @param columnIndex column identifier (corresponding to position in header of the Excel sheet)
	 * @return HTML select object
	 */
	def createPropertySelect(String name, options, matchvalue, selected, Integer columnIndex, float fuzzyTreshold = 0.1f) {
		// Determine which field in the options list matches the best with the matchvalue
		def mostsimilar = (matchvalue) ? importerService.mostSimilar(matchvalue, options, fuzzyTreshold) : ""

		def res = "<select style=\"font-size:10px\" id=\"${name}.index.${columnIndex}\" name=\"${name}.index.${columnIndex}\">"

		res += "<option value=\"dontimport\">Don't import</option>"

		options.each { f ->
			res += "<option value=\"${f.name}\""

			// mostsimilar string passed as argument or selected value passed?
            res += (mostsimilar.toString().toLowerCase() == f.name.toLowerCase() || selected.toLowerCase() == f.name.toLowerCase() ) ?
				" selected='selected'>" :
				">"

			res += (f.preferredIdentifier) ?
				"${f.name} (IDENTIFIER)</option>" :
				"${f.name}</option>"
		}

		res += "</select>"
		return res
	}

	/**
	 * @param selected selected TemplateFieldType
	 * @param custval custom value to be combined in the option(s) of the selector
	 * @param name name of the HTML select object
	 * @return HTML select object
	 *
	 * @see org.dbnp.gdt.TemplateFieldType
	 */

	def entitySelect = { attrs ->
		def sel = (attrs['selected'] == null) ? -1 : attrs['selected']
		def custval = (attrs['customvalue'] == null) ? "" : attrs['customvalue']
		def name = (attrs['name'] == null) ? -1 : attrs['name']

		def res = "<select style=\"font-size:10px\" name=\"${name}.index.${custval}\">"

		gdtService.getTemplateEntities().each { e ->
			res += "<option value\"${e.name}\""
			res += ">${e.name} bla</option>"
		}

		res += "</select>"
		out << res
	}

	/**
	 * Create a templatefieldtype selector
	 *
	 * @param selected selected TemplateFieldType
	 * @param customvalue custom value to be combined in the option(s) of the selector
	 * @param name name of the HTML select object
	 * @return HTML select object
	 *
	 * @see org.dbnp.gdt.TemplateFieldType
	 */
	def templatefieldtypeSelect = { attrs ->
		def selected = (attrs['selected'] == null) ? -1 : attrs['selected']
		def custval = (attrs['customvalue'] == null) ? "" : attrs['customvalue']
		def name = (attrs['name'] == null) ? "" : attrs['name']

		def res = "<select style=\"font-size:10px\" name=\"${name}.index.${custval}\">"

		TemplateFieldType.list().each { e ->
			res += "<option value=\"${e}\""
			res += (e == selected) ? " selected" : ""
			res += ">${e}</option>"
		}

		res += "</select>"

		out << res
	}

	/**
	 * @param cell Cell variable
	 * @return good representation of variable (instead of toString())
	 */
	def displayCell = { attrs ->
		def cell = attrs['cell']
		def df = new DataFormatter()

		switch (cell.getCellType()) {
			case Cell.CELL_TYPE_STRING: out << cell.getStringCellValue()
				break
			case Cell.CELL_TYPE_NUMERIC: out << df.formatCellValue(cell)
				break
		}
	}

}
