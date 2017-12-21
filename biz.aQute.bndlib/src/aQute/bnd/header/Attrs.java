package aQute.bnd.header;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import aQute.bnd.version.Version;

public class Attrs implements Map<String,String> {
	public interface DataType<T> {
		Type type();
	}

	public static final DataType<String>		STRING			= new DataType<String>() {

																public Type type() {
																	return Type.STRING;
																}
															};
	public static final DataType<Long>			LONG			= new DataType<Long>() {

																public Type type() {
																	return Type.LONG;
																}
															};;
	public static final DataType<Double>		DOUBLE			= new DataType<Double>() {

																public Type type() {
																	return Type.DOUBLE;
																}
															};;
	public static final DataType<Version>		VERSION			= new DataType<Version>() {

																public Type type() {
																	return Type.VERSION;
																}
															};;
	public static final DataType<List<String>>	LIST_STRING		= new DataType<List<String>>() {

																public Type type() {
																	return Type.STRINGS;
																}
															};;
	public static final DataType<List<Long>>	LIST_LONG		= new DataType<List<Long>>() {

																public Type type() {
																	return Type.LONGS;
																}
															};;
	public static final DataType<List<Double>>	LIST_DOUBLE		= new DataType<List<Double>>() {

																public Type type() {
																	return Type.DOUBLES;
																}
															};;
	public static final DataType<List<Version>>	LIST_VERSION	= new DataType<List<Version>>() {

																public Type type() {
																	return Type.VERSIONS;
																}
															};;

	public enum Type {
		STRING(null, "String"), LONG(null, "Long"), VERSION(null, "Version"), DOUBLE(null, "Double"), STRINGS(STRING,
				"List<String>"), LONGS(LONG, "List<Long>"), VERSIONS(VERSION, "List<Version>"), DOUBLES(DOUBLE,
						"List<Double>");

		Type	sub;
		String	toString;

		Type(Type sub, String toString) {
			this.sub = sub;
			this.toString = toString;
		}

		public String toString() {
			return toString;
		}

		public Type plural() {
			switch (this) {
				case DOUBLE :
					return DOUBLES;

				case LONG :
					return LONGS;
				case STRING :
					return STRINGS;
				case VERSION :
					return VERSIONS;
				default :
					return null;
			}
		}
	}

	/**
	 * <pre>
	 *  Provide-Capability ::= capability ::= name-space ::= typed-attr ::=
	 * type ::= scalar ::= capability ( ',' capability )* name-space ( ’;’
	 * directive | typed-attr )* symbolic-name extended ( ’:’ type ) ’=’
	 * argument scalar | list ’String’ | ’Version’ | ’Long’ list ::= ’List<’
	 * scalar ’>’
	 * </pre>
	 */
	private static final String	EXTENDED	= "[\\-0-9a-zA-Z\\._]+";
	private static final String	SCALAR		= "String|Version|Long|Double";
	private static final String	LIST		= "List\\s*<\\s*(" + SCALAR + ")\\s*>";
	public static final Pattern	TYPED		= Pattern
			.compile("\\s*(" + EXTENDED + ")\\s*:\\s*(" + SCALAR + "|" + LIST + ")\\s*");

	private final Map<String, String>	map;
	private final Map<String, Type>		types;
	public static final Attrs			EMPTY_ATTRS	= new Attrs(Collections.emptyMap(), Collections.emptyMap());

	private Attrs(Map<String, String> map, Map<String, Type> types) {
		this.map = map;
		this.types = types;
	}

	public Attrs() {
		this(new LinkedHashMap<String, String>(), new HashMap<String, Type>());
	}

	public Attrs(Attrs... attrs) {
		this();
		for (Attrs a : attrs) {
			if (a != null) {
				putAll(a);
			}
		}
	}

	public void putAllTyped(Map<String,Object> attrs) {

		for (Map.Entry<String,Object> entry : attrs.entrySet()) {
			Object value = entry.getValue();
			String key = entry.getKey();
			putTyped(key, value);

		}
	}

	public void putTyped(String key, Object value) {

		if (value == null) {
			put(key, null);
			return;
		}

		if (!(value instanceof String)) {
			Type type;

			if (value instanceof Collection)
				value = ((Collection< ? >) value).toArray();

			if (value.getClass().isArray()) {
				type = Type.STRINGS;
				int l = Array.getLength(value);
				StringBuilder sb = new StringBuilder();
				String del = "";
				boolean first = true;
				for (int i = 0; i < l; i++) {

					Object member = Array.get(value, i);
					if (member == null) {
						// TODO What do we do with null members?
						continue;
					} else if (first) {
						type = getObjectType(member).plural();
						first = true;
					}
					sb.append(del);
					int n = sb.length();
					sb.append(member);
					while (n < sb.length()) {
						char c = sb.charAt(n);
						if (c == '\\' || c == ',') {
							sb.insert(n, '\\');
							n++;
						}
						n++;
					}

					del = ",";
				}
				value = sb;
			} else {
				type = getObjectType(value);
			}
			key += ":" + type.toString();
		}
		put(key, value.toString());

	}

	private Type getObjectType(Object member) {
		if (member instanceof Double || member instanceof Float)
			return Type.DOUBLE;
		if (member instanceof Number)
			return Type.LONG;
		if (member instanceof Version)
			return Type.VERSION;

		return Type.STRING;
	}

	public void clear() {
		map.clear();
		types.clear();
	}

	public boolean containsKey(String name) {
		return map.containsKey(name);
	}

	@SuppressWarnings("cast")
	@Deprecated
	public boolean containsKey(Object name) {
		assert name instanceof String;
		return map.containsKey(name);
	}

	public boolean containsValue(String value) {
		return map.containsValue(value);
	}

	@SuppressWarnings("cast")
	@Deprecated
	public boolean containsValue(Object value) {
		assert value instanceof String;
		return map.containsValue(value);
	}

	public Set<java.util.Map.Entry<String,String>> entrySet() {
		return map.entrySet();
	}

	@SuppressWarnings("cast")
	@Deprecated
	public String get(Object key) {
		assert key instanceof String;
		return map.get(key);
	}

	public String get(String key) {
		return map.get(key);
	}

	public String get(String key, String deflt) {
		String s = get(key);
		if (s == null)
			return deflt;
		return s;
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

	public Set<String> keySet() {
		return map.keySet();
	}

	public String put(String key, String value) {
		if (key == null)
			return null;

		Matcher m = TYPED.matcher(key);
		if (m.matches()) {
			key = m.group(1);
			String type = m.group(2);
			Type t = Type.STRING;

			if (type.startsWith("List")) {
				type = m.group(3);
				if ("String".equals(type))
					t = Type.STRINGS;
				else if ("Long".equals(type))
					t = Type.LONGS;
				else if ("Double".equals(type))
					t = Type.DOUBLES;
				else if ("Version".equals(type))
					t = Type.VERSIONS;
			} else {
				if ("String".equals(type))
					t = Type.STRING;
				else if ("Long".equals(type))
					t = Type.LONG;
				else if ("Double".equals(type))
					t = Type.DOUBLE;
				else if ("Version".equals(type))
					t = Type.VERSION;
			}
			if (t != Type.STRING) {
				types.put(key, t);
			} else {
				types.remove(key);
			}

			// TODO verify value?
		}

		return map.put(key, value);
	}

	public Type getType(String key) {
		Type t = types.get(key);
		if (t == null)
			return Type.STRING;
		return t;
	}

	public void putAll(Attrs attrs) {
		types.keySet()
			.removeAll(attrs.map.keySet());
		map.putAll(attrs.map);
		types.putAll(attrs.types);
	}

	public void putAll(Map<? extends String, ? extends String> other) {
		if (other instanceof Attrs) {
			putAll((Attrs) other);
			return;
		}
		for (Map.Entry<? extends String, ? extends String> e : other.entrySet()) {
			put(e.getKey(), e.getValue());
		}
	}

	@SuppressWarnings("cast")
	@Deprecated
	public String remove(Object var0) {
		assert var0 instanceof String;
		types.remove(var0);
		return map.remove(var0);
	}

	public String remove(String var0) {
		types.remove(var0);
		return map.remove(var0);
	}

	public int size() {
		return map.size();
	}

	public Collection<String> values() {
		return map.values();
	}

	public String getVersion() {
		return get("version");
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		append(sb);
		return sb.toString();
	}

	public void append(StringBuilder sb) {
		try {
			String del = "";
			for (Map.Entry<String,String> e : entrySet()) {
				sb.append(del);
				append(sb, e);
				del = ";";
			}
		} catch (Exception e) {
			// Cannot happen
			e.printStackTrace();
		}
	}

	public void append(StringBuilder sb, Map.Entry<String,String> e) throws IOException {
		String key = e.getKey();
		sb.append(key);
		Type type = getType(key);
		if (type != Type.STRING) {
			sb.append(":")
				.append(type);
		}
		sb.append("=");
		OSGiHeader.quote(sb, e.getValue());
	}

	@Override
	@Deprecated
	public boolean equals(Object other) {
		return super.equals(other);
	}

	@Override
	@Deprecated
	public int hashCode() {
		return super.hashCode();
	}

	public boolean isEqual(Attrs other) {
		if (this == other)
			return true;

		if (other == null || size() != other.size())
			return false;

		if (isEmpty())
			return true;

		TreeSet<String> l = new TreeSet<String>(keySet());
		TreeSet<String> lo = new TreeSet<String>(other.keySet());
		if (!l.equals(lo))
			return false;

		for (String key : keySet()) {
			if (!Objects.equals(get(key), other.get(key))) {
				return false;
			}
			if (getType(key) != other.getType(key)) {
				return false;
			}
		}
		return true;
	}

	public Object getTyped(String adname) {
		String s = get(adname);
		if (s == null)
			return null;

		Type t = getType(adname);
		return convert(t, s);
	}

	@SuppressWarnings("unchecked")
	public <T> T getTyped(DataType<T> type, String adname) {
		String s = get(adname);
		if (s == null)
			return null;

		Type t = getType(adname);
		if (t != type.type())
			throw new IllegalArgumentException(
					"For key " + adname + ", expected " + type.type() + " but had a " + t + ". Value is " + s);

		return (T) convert(t, s);
	}

	public static Type toType(String type) {
		for (Type t : Type.values()) {
			if (t.toString.equals(type))
				return t;
		}
		return null;
	}

	public static Object convert(String t, String s) {
		if (s == null)
			return null;

		Type type = toType(t);
		if (type == null)
			return s;

		return convert(type, s);
	}

	public static Object convert(Type t, String s) {
		if (t.sub == null) {
			switch (t) {
				case STRING :
					return s;
				case LONG :
					return Long.parseLong(s.trim());
				case VERSION :
					return Version.parseVersion(s);
				case DOUBLE :
					return Double.parseDouble(s.trim());

				case DOUBLES :
				case LONGS :
				case STRINGS :
				case VERSIONS :
					// Cannot happen since the sub is null
					return null;
			}
			return null;
		}
		List<Object> list = new ArrayList<Object>();

		List<String> split = splitListAttribute(s);
		for (String p : split)
			list.add(convert(t.sub, p));
		return list;
	}

	static List<String> splitListAttribute(String input) throws IllegalArgumentException {
		List<String> result = new LinkedList<String>();

		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			switch (c) {
				case '\\' :
					i++;
					if (i >= input.length())
						throw new IllegalArgumentException("Trailing blackslash in multi-valued attribute value");
					c = input.charAt(i);
					builder.append(c);
					break;
				case ',' :
					result.add(builder.toString());
					builder = new StringBuilder();
					break;
				default :
					builder.append(c);
					break;
			}
		}
		result.add(builder.toString());
		return result;
	}

	/**
	 * Merge the attributes
	 */

	public void mergeWith(Attrs other, boolean override) {
		for (Map.Entry<String,String> e : other.entrySet()) {
			String key = e.getKey();
			if (override || !containsKey(key)) {
				map.put(key, e.getValue());
				Type t = other.getType(key);
				if (t != Type.STRING) {
					types.put(key, t);
				} else {
					types.remove(key);
				}
			}
		}
	}

	/**
	 * Check if a directive, if so, return directive name otherwise null
	 */
	public static String toDirective(String key) {
		if (key == null || !key.endsWith(":"))
			return null;

		return key.substring(0, key.length() - 1);
	}

	public static Attrs create(String key, String value) {
		Attrs attrs = new Attrs();
		attrs.put(key, value);
		return attrs;
	}

	public Attrs with(String key, String value) {
		put(key, value);
		return this;
	}
}
