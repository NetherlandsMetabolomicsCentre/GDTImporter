<%
	/**
	 * Wizard index page
	 *
	 * @author Jeroen Wesbeek
	 * @since 20101206
	 *
	 * Revision information:
	 * $Rev: 1489 $
	 * $Author: t.w.abma@umcutrecht.nl $
	 * $Date: 2011-02-03 17:07:42 +0100 (Thu, 03 Feb 2011) $
	 */
%>
<html>
<head>
	<meta name="layout" content="main"/>
	<link rel="stylesheet" href="${resource(dir: 'css', file: 'ajaxflow.css')}"/>
	<link rel="stylesheet" href="${resource(dir: 'css', file: 'importer.css')}"/>
	<link rel="stylesheet" href="${resource(dir: 'css', file: 'studywizard.css')}"/>
	<link rel="stylesheet" href="${resource(dir: 'css', file: 'table-editor.css', plugin: 'gdt')}"/>

	<script type="text/javascript" src="${resource(dir: 'js', file: 'studywizard.js')}"></script>

	<g:if env="production">
	<script type="text/javascript" src="${resource(dir: 'js', file: 'jquery.qtip-1.0.0-rc3.min.js', plugin: 'gdt')}"></script>
	<script type="text/javascript" src="${resource(dir: 'js', file: 'SelectAddMore.min.js', plugin: 'gdt')}"></script>
	<script type="text/javascript" src="${resource(dir: 'js', file: 'ajaxupload.3.6.js')}"></script>
	<script type="text/javascript" src="${resource(dir: 'js', file: 'ontology-chooser.min.js', plugin: 'gdt')}"></script>
	<script type="text/javascript" src="${resource(dir: 'js', file: 'table-editor.js', plugin: 'gdt')}"></script>
	<script type="text/javascript" src="${resource(dir: 'js', file: 'tooltips.js', plugin: 'gdt')}"></script>
	</g:if><g:else>
	<script type="text/javascript" src="${resource(dir: 'js', file: 'jquery.qtip-1.0.0-rc3.js', plugin: 'gdt')}"></script>
	<script type="text/javascript" src="${resource(dir: 'js', file: 'SelectAddMore.js', plugin: 'gdt')}"></script>
	<script type="text/javascript" src="${resource(dir: 'js', file: 'ajaxupload.3.6.js')}"></script>
	<script type="text/javascript" src="${resource(dir: 'js', file: 'ontology-chooser.js', plugin: 'gdt')}"></script>
	<script type="text/javascript" src="${resource(dir: 'js', file: 'table-editor.js', plugin: 'gdt')}"></script>
	<script type="text/javascript" src="${resource(dir: 'js', file: 'tooltips.js', plugin: 'gdt')}"></script>
	</g:else>

</head>
<body>
<g:render template="common/ajaxflow"/>
</body>
</html>
