/*******************************************************************************
 * Copyright 2012 University of Southern California
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * 	http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * This code was developed by the Information Integration Group as part 
 * of the Karma project at the Information Sciences Institute of the 
 * University of Southern California.  For more information, publications, 
 * and related projects, please see: http://www.isi.edu/integration
 ******************************************************************************/

package edu.isi.karma.modeling.alignment;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jgrapht.graph.DirectedWeightedMultigraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.isi.karma.rep.alignment.ColumnNode;
import edu.isi.karma.rep.alignment.Link;
import edu.isi.karma.rep.alignment.LiteralNode;
import edu.isi.karma.rep.alignment.Node;

public class GraphVizUtil {

	private static Logger logger = LoggerFactory.getLogger(GraphVizUtil.class);

	private static double roundTwoDecimals(double d) {
        DecimalFormat twoDForm = new DecimalFormat("#.##");
        return Double.valueOf(twoDForm.format(d));
	}
	
	public static String getLocalName(String uri) {

		if (uri == null)
			return "";
		
		String localName = uri;
		
		if (uri.contains("#") && !uri.endsWith("#"))
			localName = uri.substring(uri.lastIndexOf('#') + 1);
		else {
			if (uri.endsWith("/"))
				uri = uri.substring(0, uri.length() - 2);
			if (uri.contains("/"))
				localName = uri.substring(uri.lastIndexOf('/') + 1);
		}
		return localName;
	}
	
	private static String getModelIds(Set<String> modelIds) {
		String label = "";
		if (modelIds == null || modelIds.size() == 0)
			return label;
		label += "[";
		for (String pId : modelIds)
			label += pId + ",";
		if (label.endsWith(","))
			label = label.substring(0, label.length() - 1);
		label += "]";
		return label;
	}
	
	private static org.kohsuke.graphviz.Graph convertToGraphviz(
			DirectedWeightedMultigraph<Node, Link> graph, 
			Map<ColumnNode, ColumnNode> mappingToSourceColumns,
			boolean onlyAddPatterns,
			boolean showNodeMetaData,
			boolean showLinkMetaData) {

		String metaDataSeparator = "\n";
		
		org.kohsuke.graphviz.Graph gViz = new org.kohsuke.graphviz.Graph();

		if (graph == null)
			return gViz;

		org.kohsuke.graphviz.Style internalNodeStyle = new org.kohsuke.graphviz.Style();
//		internalNodeStyle.attr("shape", "circle");
		internalNodeStyle.attr("style", "filled");
		internalNodeStyle.attr("color", "white");
		//internalNodeStyle.attr("fontsize", "10");
		internalNodeStyle.attr("fillcolor", "lightgray");
		
//		org.kohsuke.graphviz.Style inputNodeStyle = new org.kohsuke.graphviz.Style();
//		inputNodeStyle.attr("shape", "plaintext");
//		inputNodeStyle.attr("style", "filled");
//		inputNodeStyle.attr("fillcolor", "#3CB371");
//
//		org.kohsuke.graphviz.Style outputNodeStyle = new org.kohsuke.graphviz.Style();
//		outputNodeStyle.attr("shape", "plaintext");
//		outputNodeStyle.attr("style", "filled");
//		outputNodeStyle.attr("fillcolor", "gold");

		org.kohsuke.graphviz.Style parameterNodeStyle = new org.kohsuke.graphviz.Style();
		parameterNodeStyle.attr("shape", "plaintext");
		parameterNodeStyle.attr("style", "filled");
		parameterNodeStyle.attr("fillcolor", "gold");

		org.kohsuke.graphviz.Style literalNodeStyle = new org.kohsuke.graphviz.Style();
		literalNodeStyle.attr("shape", "plaintext");
		literalNodeStyle.attr("style", "filled");
		literalNodeStyle.attr("fillcolor", "#CC7799");

		org.kohsuke.graphviz.Style edgeStyle = new org.kohsuke.graphviz.Style();
		edgeStyle.attr("color", "brown");
		//edgeStyle.attr("fontsize", "10");
		edgeStyle.attr("fontcolor", "black");
		
		HashMap<Node, org.kohsuke.graphviz.Node> nodeIndex = new HashMap<Node, org.kohsuke.graphviz.Node>();
		
		for (Link e : graph.edgeSet()) {
			
			if (onlyAddPatterns)
				if (e.getModelIds() == null || e.getModelIds().isEmpty())
					continue;
			
			Node source = e.getSource();
			Node target = e.getTarget();
			
			org.kohsuke.graphviz.Node n = nodeIndex.get(source);
			String sourceId = source.getId();
			String sourceUri = sourceId;//source.getLabel().getUri();
			String sourceLocalName = getLocalName(sourceUri);
			String sourceLabel;
			if (n == null) {
				n = new org.kohsuke.graphviz.Node();
//				label = (uri == null || uri.trim().length() == 0?id:uri));
				sourceLabel = (sourceLocalName == null || sourceLocalName.trim().length() == 0?sourceId:sourceLocalName);
				if (showNodeMetaData) 
					sourceLabel += metaDataSeparator + getModelIds(source.getModelIds()); 
				n.attr("label", sourceLabel);
				nodeIndex.put(source, n);
			
				if (source instanceof ColumnNode)  // attribute
					gViz.nodeWith(parameterNodeStyle);
				else if (source instanceof LiteralNode)  // literal
					gViz.nodeWith(literalNodeStyle);
				else  // internal node
					gViz.nodeWith(internalNodeStyle);
					
				gViz.node(n);
			}

			n = nodeIndex.get(target);
			String targetId = target.getId();
			String targetUri = targetId;//target.getLabel().getUri();
			String targetLocalName = getLocalName(targetUri);
			String targetLabel;
			if (n == null) {
				n = new org.kohsuke.graphviz.Node();
//				label = (uri == null || uri.trim().length() == 0?id:uri));
				targetLabel = (targetLocalName == null || targetLocalName.trim().length() == 0?targetId:targetLocalName);
				if (target instanceof ColumnNode) {
					ColumnNode mappedColumn = mappingToSourceColumns.get(target);
					targetLabel = mappedColumn.getColumnName();
				}
				if (showNodeMetaData) 
					targetLabel += metaDataSeparator + getModelIds(target.getModelIds()); 
				n.attr("label", targetLabel);
				nodeIndex.put(target, n);
			
				if (target instanceof ColumnNode)  // attribute
					gViz.nodeWith(parameterNodeStyle);
				else if (target instanceof LiteralNode)  // literal
					gViz.nodeWith(literalNodeStyle);
				else  // internal node
					gViz.nodeWith(internalNodeStyle);
					
				gViz.node(n);
			}
			
			org.kohsuke.graphviz.Edge edge = new org.kohsuke.graphviz.Edge(nodeIndex.get(source), nodeIndex.get(target));
			
			String edgeId = e.getId();
			String edgeUri = e.getLabel().getUri();
			String edgeLocalName = getLocalName(edgeUri);
			String edgeLabel = (edgeLocalName == null?edgeId:edgeLocalName);

			if (showLinkMetaData) {
				edgeLabel += metaDataSeparator;
				edgeLabel += "w=" + roundTwoDecimals(e.getWeight());
				edgeLabel += metaDataSeparator;
				edgeLabel += getModelIds(e.getModelIds());
			}

			edge.attr("label", edgeLabel);
			gViz.edgeWith(edgeStyle);
			gViz.edge(edge);
		}


		return gViz;
	}

	public static void exportJGraphToGraphviz(
			DirectedWeightedMultigraph<Node, Link> graph, 
			String label, 
			boolean onlyAddPatterns,
			boolean showNodeMetaData,
			boolean showLinkMetaData,
			String filename) throws IOException {
		
		logger.info("exporting the graph to graphviz ...");
		
		org.kohsuke.graphviz.Graph graphViz = 
				convertToGraphviz(graph, null, onlyAddPatterns, showNodeMetaData, showLinkMetaData);;
		graphViz.attr("fontcolor", "blue");
		graphViz.attr("remincross", "true");
		graphViz.attr("label", label == null ? "" : label);

		OutputStream out = new FileOutputStream(filename);
		graphViz.writeTo(out);

		logger.info("export is done.");
	}
	
	public static void exportSemanticModelToGraphviz(
			SemanticModel model, 
			boolean showNodeMetaData, 
			boolean showLinkMetaData,
			String filename) throws IOException {
		
		OutputStream out = new FileOutputStream(filename);
		org.kohsuke.graphviz.Graph graphViz = new org.kohsuke.graphviz.Graph();
		
		graphViz.attr("fontcolor", "blue");
		graphViz.attr("remincross", "true");
		graphViz.attr("label", model.getName() == null ? "" : model.getName());
//		graphViz.attr("page", "8.5,11");

		org.kohsuke.graphviz.Graph gViz = 
				convertToGraphviz(model.getGraph(), model.getMappingToSourceColumns(), false, showNodeMetaData, showLinkMetaData);
		gViz.attr("label", "model");
		gViz.id("cluster");
		graphViz.subGraph(gViz);
		graphViz.writeTo(out);
		out.close();
	}
	
	public static void exportSemanticModelsToGraphviz(
			Map<String, SemanticModel> models, 
			String label, 
			String filename) throws IOException {
		
		org.kohsuke.graphviz.Graph graphViz = new org.kohsuke.graphviz.Graph();
		graphViz.attr("fontcolor", "blue");
		graphViz.attr("remincross", "true");
		graphViz.attr("label", label == null ? "" : label);
		
		org.kohsuke.graphviz.Graph cluster = null;
		int counter = 0;
		
		boolean showNodeMetaData;
		boolean showLinkMetaData;
		if (models != null) {
			for(Entry<String,SemanticModel> entry : models.entrySet()) {
				if (entry.getKey() == "1-correct model") {
					showNodeMetaData = false;
					showLinkMetaData = false; 
				} else {
					showNodeMetaData = true;
					showLinkMetaData = true;
				}
				cluster = GraphVizUtil.convertToGraphviz(entry.getValue().getGraph(), 
						entry.getValue().getMappingToSourceColumns(), false, showNodeMetaData, showLinkMetaData);
				cluster.id("cluster_" + counter);
				cluster.attr("label", entry.getKey());
				graphViz.subGraph(cluster);
				counter++;
			}
		}

		OutputStream out = new FileOutputStream(filename);
		graphViz.writeTo(out);
	}

}