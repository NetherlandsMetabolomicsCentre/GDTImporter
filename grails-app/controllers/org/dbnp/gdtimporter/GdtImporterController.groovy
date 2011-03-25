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
import grails.converters.JSON
import org.codehaus.groovy.grails.plugins.web.taglib.ValidationTagLib
import grails.plugins.springsecurity.Secured
import org.codehaus.groovy.grails.commons.ApplicationHolder as AH

@Secured(['IS_AUTHENTICATED_REMEMBERED'])
class GdtImporterController {
    def authenticationService
	def fileService
	def gdtImporterService
    def gdtService
    def validationTagLib = new ValidationTagLib()

    /**
	 * index method, redirect to the webflow
	 * @void
	 */
	def index = {
		redirect(action: 'pages', params: params)
	}

    /**
	 * WebFlow definition
	 * @void
	 */
	def pagesFlow = {
		// start the flow
		onStart {		

			// define variables in the flow scope which is availabe
			// throughout the complete webflow also have a look at
			// the Flow Scopes section on http://www.grails.org/WebFlow
			//
			// The following flow scope variables are used to generate
			// wizard tabs. Also see common/_tabs.gsp for more information
			flow.page = 0
			flow.pages = [
				[title: 'Import file'],
				[title: 'Assign properties'],
				[title: 'Check imported data'],
				[title: 'Done']
			]
			flow.cancel = true;
			flow.quickSave = true;

            // Get parent entity class name from params or default to 'Study'

            flow.parentEntityClassName = params.parentEntityClassName ?: 'dbnp.studycapturing.Study'

			success()
		}

		// render the main wizard page which immediately
		// triggers the 'next' action (hence, the main
		// page dynamically renders the study template
		// and makes the flow jump to the study logic)
		mainPage {
			render(view: "/gdtImporter/index")
			onRender {

				// let the view know we're in page 1
				flow.page = 1
				success()
			}
			on("next").to "pageOne"
		}

		// File import and entity template selection page
		pageOne {
			render(view: "_page_one")
			onRender {
				log.info ".entering import wizard"

                flow.page = 1
                flow.gdtImporter_fuzzymatching = "false"

                // Get a list of parent entities the current logged in user owns
                def domainClass                     = AH.application.getDomainClass(flow.parentEntityClassName)
                def parentEntityReferenceInstance   = domainClass.referenceInstance
                def userParentEntities              = parentEntityReferenceInstance.findAllWhere(owner: authenticationService.loggedInUser)

                flow.gdtImporter_parentEntityReferenceInstance  = parentEntityReferenceInstance
                flow.gdtImporter_userParentEntities             = userParentEntities
                flow.gdtImporter_parentEntityClassName          = domainClass.shortName

				success()
			}

            on('temp').to 'pageOne'

			on("refresh") {

				if (params.entity) {
					flash.gdtImporter_datatemplates = Template.findAllByEntity(gdtService.getInstanceByEntity(params.entity.decodeURL()))
				}

				flash.gdtImporter_params = params

				// If the file already exists an "existing*" string is added, but we don't
				// want that after a refresh of the first step in the import wizard, so remove
				// that string
				flash.gdtImporter_params.importfile = params.importfile.replace('existing*', '')

				success()
			}.to "pageOne"

			on("next") {
				flash.wizardErrors = [:]
				flash.gdtImporter_params = params
				flash.gdtImporter_params.importfile = params.importfile.replace('existing*', '')


                if (!flash.gdtImporter_params.importfile) {
                    log.error('.gdtImporterWizard [pageOne] no file specified.')
                    error()
                } else {
                    // TODO: remove this hack
                    flash.gdtImporter_params.importfile = new XmlSlurper().parseText(flash.gdtImporter_params.importfile[flash.gdtImporter_params.importfile.indexOf('<pre')..-1]).toString()
                }

				if (params.entity) {
					flash.gdtImporter_datatemplates = Template.findAllByEntity(gdtService.getInstanceByEntity(params.entity.decodeURL()))
					def gdtImporter_entity_type = gdtService.decryptEntity(params.entity.decodeURL()).toString().split(/\./)
					flow.gdtImporter_entity_type = gdtImporter_entity_type[gdtImporter_entity_type.size()-1]
				}

                // get selected instance of parent entity through a reference of
                // parent entity's domain class
				flow.gdtImporter_parentEntity = flow.gdtImporter_parentEntityReferenceInstance.get(params.parentEntity.id)

				// Trying to import data into an existing parent entity?
				if (flow.gdtImporter_parentEntity)
					if (flow.gdtImporter_parentEntity.canWrite(authenticationService.getLoggedInUser()))
						fileImportPage(flow, flash, params) ? success() : error()
					else {
						log.error ".importer wizard wrong permissions"
						this.appendErrorMap(['error': "You don't have the right permissions"], flash.wizardErrors)
						error()
					}
				else {
					fileImportPage(flow, flash, params) ? success() : error()
				}

			}.to "pageTwo"
		}

		// Property to column assignment page
		pageTwo {
			render(view: "_page_two")
			onRender {
				log.info ".import wizard properties page"

				def template = Template.get(flow.gdtImporter_template_id)

				flow.gdtImporter_importmappings = GDTImportMapping.findAllByTemplate(template)

				flow.page = 2
				success()
			}
			on("refresh") {
				def template = Template.get(flow.gdtImporter_template_id)
				flow.gdtImporter_importmappings = GDTImportMapping.findAllByTemplate(template)

				// a name was given to the current property mapping, try to store it
				if (params.mappingname) {
					flash.gdtImporter_columnproperty = params.columnproperty
					propertiesSaveImportMappingPage(flow, flash, params)
				} else // trying to load an existing import mapping
                    if (params.importmapping_id) {
                        propertiesLoadImportMappingPage(flow, flash, params)
                    }

                flow.gdtImporter_fuzzymatching = (params.fuzzymatching == "true") ? "true" : "false"

				success()
			}.to "pageTwo"

			on("next") {
				flow.gdtImporter_fuzzymatching = "false"
				if (propertiesPage(flow, flash, params)) {
					success()
				} else {
					log.error ".import wizard, properties are set wrong"
					error()
				}
			}.to "pageThree"
			on("previous").to "pageOne"
		}

		// Mapping page
		pageThree {
			render(view: "_page_three")
			onRender {
				log.info ".import wizard mapping page"

				flow.page = 3
				success()
			}
			on("refresh") {
				success()
			}.to "pageThree"
			on("next") {

                def entityList      = []
                def failedFields    = []

                flash.wizardErrors  = [:]

                if (flow.gdtImporter_failedFields) {

                    gdtImporterService.updateDataMatrixFromParams(flow.gdtImporter_dataMatrix, params, flow.gdtImporter_entityList, flow.gdtImporter_header)

                    (entityList, failedFields) = gdtImporterService.getDataMatrixAsEntityList(
                            flow.gdtImporter_entity,
                            Template.get(flow.gdtImporter_template_id),
                            flow.gdtImporter_dataMatrix,
                            flow.gdtImporter_header)

                    flow.gdtImporter_entityList = entityList

                }

                def duplicateFailedFields = gdtImporterService.detectUniqueConstraintViolations(flow.gdtImporter_entityList, flow.gdtImporter_parentEntity)

                if (duplicateFailedFields) {
                    appendErrorMap(['duplicates': "Some of the fields that should be unique are duplicates."], flash.wizardErrors)
                }

                failedFields += duplicateFailedFields

                if (failedFields) {
                    flow.gdtImporter_failedFields = failedFields
                    appendErrorMap(['error': "One or more properties could not be set. Please correct the cells marked red."], flash.wizardErrors)
                    log.error ".import wizard mapping error, could not validate all entities"
                    error()
                } else {

                    flow.page = 4

                    gdtImporterService.addEntitiesToParentEntity(flow.gdtImporter_entityList, flow.gdtImporter_parentEntity, )

                    if (!flow.gdtImporter_parentEntity.save()) {
                        log.error ".gdtImporter [pageThree] could not save parent entity."
                        error()
                    } else success()
                }
			}.to "finalPage"
			on("previous").to "pageTwo"
		}

		// render errors
		error {
			render(view: "_error")
			onRender {

				// set page to 4 so that the navigation
				// works (it is disabled on the final page)
				flow.page = 4
			}
			on("tryAgain").to "pageOne"
		}

		// last wizard page
		finalPage {
			render(view: "_final_page")
			onRender {
				success()
			}
			onEnd {
				// clean flow scope
				flow.clear()
			}
		}
	}

	/**
	 * Return templates which belong to a certain entity type
	 *
	 * @param entity entity name string (Sample, Subject, Study et cetera)
	 * @return JSON object containing the found templates
	 */
	def ajaxGetTemplatesByEntity = {
		// fetch all templates for a specific entity
		def templates = Template.findAllByEntity(gdtService.getInstanceByEntity(params.entity.decodeURL()))

		// render as JSON
		render templates as JSON
	}

	/**
	 * Handle the file import page.
	 *
	 * @param Map LocalAttributeMap (the flow scope)
	 * @param Map GrailsParameterMap (the flow parameters = form data)
	 * @returns boolean true if correctly validated, otherwise false
	 */
	boolean fileImportPage(flow, flash, params) {
		def importedFile = fileService.get(params['importfile'])
        def workbook

		if (importedFile.exists()) {
			try {
				workbook = gdtImporterService.getWorkbook(new FileInputStream(importedFile))
			} catch (Exception e) {
				log.error ".importer wizard could not load file: " + e
				this.appendErrorMap(['error': "Wrong file (format), the importer requires an Excel file as input"], flash.wizardErrors)
				return false
            }
		}

		if (params.entity && params.template_id) {

			def entityName = gdtService.decryptEntity(params.entity.decodeURL())
			def entityClass = gdtService.getInstanceByEntityName(entityName)

			flow.gdtImporter_template_id = params.template_id
			flow.gdtImporter_sheetindex = params.sheetindex.toInteger() - 1 // 0 == first sheet
			flow.gdtImporter_dataMatrix_start = params.dataMatrix_start.toInteger() - 1 // 0 == first row
			flow.gdtImporter_headerrow = params.headerrow.toInteger()
			flow.gdtImporter_entityclass = entityClass
			flow.gdtImporter_entity = gdtService.cachedEntities.find { it.entity == entityName }

			// Get the header from the Excel file using the arguments given in the first step of the wizard
			flow.gdtImporter_header = gdtImporterService.getHeader(workbook,
				flow.gdtImporter_sheetindex,
				flow.gdtImporter_headerrow,
				flow.gdtImporter_dataMatrix_start,
				entityClass)

			// Load a preview of the data
            flow.gdtImporter_dataMatrix = gdtImporterService.getDataMatrix(
				workbook, flow.gdtImporter_header,
				flow.gdtImporter_sheetindex,
				flow.gdtImporter_dataMatrix_start)

			flow.gdtImporter_templates = Template.get(flow.gdtImporter_template_id)
			flow.gdtImporter_allfieldtypes = "true"

            fileService.delete params['importfile']

			return true
		}


		log.error ".importer wizard not all fields are filled in"
		this.appendErrorMap(['error': "Not all fields are filled in, please fill in or select all fields"], flash.wizardErrors)
		return false
	}

	/**
	 * Load an existing import mapping
	 *
	 * @param Map LocalAttributeMap (the flow scope)
	 * @param Map GrailsParameterMap (the flow parameters = form data)
	 * @returns return value not used
	 */
	def propertiesLoadImportMappingPage(flow, flash, params) {
		def im = ImportMapping.get(params.importmapping_id.toInteger())
		im.refresh()

		im.mappingcolumns.each { gdtmappingcolumn ->

			flow.gdtImporter_header[gdtmappingcolumn.index.toInteger()] = gdtmappingcolumn

		}
	}

	/**
	 * Save the properties as an import mapping.
	 *
	 * @param Map LocalAttributeMap (the flow scope)
	 * @param Map GrailsParameterMap (the flow parameters = form data)
	 * @returns return value not used
	 */
	def propertiesSaveImportMappingPage(flow, flash, params) {
		flash.wizardErrors = [:]
		def isPreferredIdentifier = false

		// Find actual Template object from the chosen template name
		def template = Template.get(flow.gdtImporter_template_id)

		// Create new GDTImportMapping instance and persist it
		def im = new GDTImportMapping(name: params.mappingname, entity: flow.gdtImporter_entityclass, template: template).save()

		params.columnproperty.index.each { columnindex, property ->
			// Create an actual class instance of the selected entity with the selected template
			// This should be inside the closure because in some cases in the advanced importer, the fields can have different target entities
			//def entityClass = gdtService.getInstanceByEntityName(flow.gdtImporter_header[columnindex.toInteger()].entity.getName())
			def entityObj = flow.gdtImporter_entityclass.newInstance(template: template)

			def dontimport = (property == "dontimport")

			// Loop through all fields and find the preferred identifier
			entityObj.giveFields().each {
				isPreferredIdentifier = it.preferredIdentifier && (it.name == property)
			}

			// Create new GDTMappingColumn instance
			def mc = new GDTMappingColumn(gdtimportmapping: im,
				name: flow.gdtImporter_header[columnindex.toInteger()].name,
				property: property,
				index: columnindex,
				entityclass: flow.gdtImporter_entityclass,
				templatefieldtype: entityObj.giveFieldType(property),
				dontimport: dontimport,
				identifier: isPreferredIdentifier)

			// Save gdtmappingcolumn
			if (mc.validate()) {
				im.addToGdtmappingcolumns(mc)
			}
			else {
				mc.errors.allErrors.each {
					println it
				}
			}

			// Save gdtimportmapping
			if (im.validate()) {
				try {
					im.save(flush: true)
				} catch (Exception e) {
					//getNextException
					log.error "importer wizard save gdtimportmapping error: " + e
				}
			}
			else {
				im.errors.allErrors.each {
					println it
				}
			}

		}
	}

	/**
	 * Handle the property mapping page.
	 *
	 * @param Map LocalAttributeMap (the flow scope)
	 * @param Map GrailsParameterMap (the flow parameters = form data)
	 * @returns boolean true if correctly validated, otherwise false
	 */
	boolean propertiesPage(flow, flash, params) {
		flash.wizardErrors = [:]

		// Find actual Template object from the chosen template name
		def template = Template.get(flow.gdtImporter_template_id)

        // Create an actual class instance of the selected entity with the selected template
        def entityObj = flow.gdtImporter_entityclass.newInstance(template: template)

		params.columnproperty.index.each { columnIndex, property ->

            def column = flow.gdtImporter_header[columnIndex.toInteger()]

			// Store the selected property for this column into the column map for the gdtImporterService
			column.property = property

			// Look up the template field type of the target TemplateField and store it also in the map
			column.templatefieldtype = entityObj.giveFieldType(property)

			// Is a "Don't import" property assigned to the column?
			column.dontimport = (property == "dontimport")

		}

		// Import the workbook and store the table with entity records and store the failed cells
		def (entityList, failedFields) = gdtImporterService.getDataMatrixAsEntityList(flow.gdtImporter_entity, template,
			flow.gdtImporter_dataMatrix,
			flow.gdtImporter_header)

		flow.gdtImporter_entityList = entityList
        flow.gdtImporter_failedFields = failedFields

        return true
	}

	/**
	 * append errors of a particular object to a map
	 * @param object
	 * @param map linkedHashMap
	 * @void
	 */
	def appendErrors(object, map) {
		this.appendErrorMap(getHumanReadableErrors(object), map)
	}

	def appendErrors(object, map, prepend) {
		this.appendErrorMap(getHumanReadableErrors(object), map, prepend)
	}

	/**
	 * append errors of one map to another map
	 * @param map linkedHashMap
	 * @param map linkedHashMap
	 * @void
	 */
	def appendErrorMap(map, mapToExtend) {
		map.each() {key, value ->
			mapToExtend[key] = ['key': key, 'value': value, 'dynamic': false]
		}
	}

	def appendErrorMap(map, mapToExtend, prepend) {
		map.each() {key, value ->
			mapToExtend[prepend + key] = ['key': key, 'value': value, 'dynamic': true]
		}
	}

	/**
	 * transform domain class validation errors into a human readable
	 * linked hash map
	 * @param object validated domain class
	 * @return object  linkedHashMap
	 */
	def getHumanReadableErrors(object) {
		def errors = [:]
		object.errors.getAllErrors().each() { error ->
			// error.codes.each() { code -> println code }

			// generally speaking g.message(...) should work,
			// however it fails in some steps of the wizard
			// (add event, add assay, etc) so g is not always
			// availably. Using our own instance of the
			// validationTagLib instead so it is always
			// available to us
			errors[error.getArguments()[0]] = validationTagLib.message(error: error)
		}

		return errors
	}
}
