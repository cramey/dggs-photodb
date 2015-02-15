package gov.alaska.dggs;

import java.util.Properties;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.queryparser.flexible.standard.config.StandardQueryConfigHandler;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.TermRangeQuery;

// This class uses Lucene's query parser to generate
// PostgreSQL/MyBatis compatiable where clauses.
public class SQLQueryParser {
	// fields stores a list of fields the user is allowed
	// to specify. The "value" should be either a PostgreSQL
	// config for tsvector, or "numeric" or "date"
	// Support for further classes could be easily added.
	private Properties fields;
	private StringBuilder where;
	private StringBuilder order;
	private int params_count;
	private HashMap<String, String> params;


	public SQLQueryParser()
	{
		reset();
	}


	public SQLQueryParser(Properties fields)
	{
		setFields(fields);
		reset();
	}


	public void reset()
	{
		where = new StringBuilder();
		order = new StringBuilder();
		params_count = 0;
		params = new HashMap<String, String>();
	}


	public Properties getFields()
	{
		return fields;
	}


	public void setFields(Properties fields)
	{
		this.fields = fields;
	}


	public String getWhereClause()
	{
		return where.toString();
	}

	public String getOrderClause()
	{
		return order.toString();
	}

	public Map<String,String> getParameters()
	{
		return params;
	}


	public void parse(String query, String defaultfield) throws Exception
	{
		StandardQueryParser parser = new StandardQueryParser();
		parser.setDefaultOperator(StandardQueryConfigHandler.Operator.AND);

		build(parser.parse(query, defaultfield));
	}


	private void build(BooleanQuery query) throws Exception
	{
		BooleanClause[] clauses = query.getClauses();
		for(int i = 0; i < clauses.length; i++){
			Query q = clauses[i].getQuery();

			switch(clauses[i].getOccur()){
				case MUST:
					if(i > 0) where.append(" AND ");
				break;
				
				case MUST_NOT:
					if(i > 0) where.append(" AND "); 
					where.append("NOT ");
				break;

				case SHOULD:
					if(i > 0) where.append(" OR ");
				break;
			}

			if(q instanceof BooleanQuery) where.append("(");
			build(q);
			if(q instanceof BooleanQuery) where.append(")");
		}
	}


	private void build(TermQuery query) throws Exception
	{
		Term term = query.getTerm();
		if(term == null) throw new Exception("Invalid term.");

		String field = term.field().toLowerCase();
		String type = fields.getProperty(field);
		if(type == null) throw new Exception("Invalid field: " + field);

		String text = term.text();
		addTerm(field, type, text);
	}


	private void build(PrefixQuery query) throws Exception
	{
		Term term = query.getPrefix();
		if(term == null) throw new Exception("Invalid term");

		String field = term.field().toLowerCase();
		String type = fields.getProperty(field);
		if(type == null) throw new Exception("Invalid field: " + field);
		if("date".equalsIgnoreCase(type) || "numeric".equalsIgnoreCase(type)){
			throw new Exception("Field does not support prefix query: " + field);
		}
		String text = term.text() + ":*";
		addTerm(field, type, text);
	}


	private void build(TermRangeQuery query) throws Exception
	{
		String field = query.getField().toLowerCase();
		String type = fields.getProperty(field);

		if(type == null) throw new Exception("Invalid field: " + field);

		if(query.getUpperTerm() == null || query.getLowerTerm() == null){
			throw new Exception("Invalid range for field: " + field);
		}

		String upper = query.getUpperTerm().utf8ToString();
		String lower = query.getLowerTerm().utf8ToString();
		if("date".equalsIgnoreCase(type) || "numeric".equalsIgnoreCase(type)){
			where.append(field);
			where.append(" BETWEEN ");
			where.append("#{param" + String.valueOf(params_count + 1) + "}::" + type);
			where.append(" AND ");
			where.append("#{param" + String.valueOf(params_count + 2) + "}::" + type);
			params.put("param" + String.valueOf(params_count + 1), lower);
			params.put("param" + String.valueOf(params_count + 2), upper);
			params_count += 2;
		} else {
			throw new Exception("Field does not support range query: " + field);
		}
	}


	private void build(Query query) throws Exception
	{
		if(query instanceof BooleanQuery){
			build((BooleanQuery)query);
		} else if(query instanceof TermQuery){
			build((TermQuery)query);
		} else if(query instanceof PrefixQuery){
			build((PrefixQuery)query);
		} else if(query instanceof TermRangeQuery){
			build((TermRangeQuery)query);
		} else {
			throw new Exception(
				"Unsupported query operation: ("
				+ query.getClass().getName() + "): "
				+ query.toString()
			);
		}
	}


	private void addTerm(String field, String type, String text)
	{
		params_count++;
		if("date".equalsIgnoreCase(type) || "numeric".equalsIgnoreCase(type)){
			where.append(field);
			where.append(" = #{param" + String.valueOf(params_count) + "}::" + type);
			params.put("param" + String.valueOf(params_count), text);
		} else {
			where.append(field);
			where.append(
				" @@ to_tsquery('" + type + "', #{param"
				+ String.valueOf(params_count) + "})"
			);

			if(order.length() > 0){ order.append(", "); }
			order.append(
				"ts_rank('" + field + "', to_tsquery('"
				+ type + "', #{param" + String.valueOf(params_count)
				+ "})) DESC"
			);

			params.put(
				"param" + String.valueOf(params_count),
				text.replace(" ", "&")
			);
		}
	}
}
