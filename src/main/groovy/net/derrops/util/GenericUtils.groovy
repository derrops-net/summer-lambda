package net.derrops.util

import java.nio.charset.StandardCharsets

class GenericUtils {

    static <T> List<List<T>> split(List<T> list, int size)
            throws NullPointerException, IllegalArgumentException {
        if (list == null) {
            throw new NullPointerException("The list parameter is null.");
        }

        if (size <= 0) {
            throw new IllegalArgumentException(
                    "The size parameter must be more than 0.");
        }

        List<List<T>> result = new ArrayList<List<T>>(size);

        for (int i = 0; i < size; i++) {
            result.add(new ArrayList<T>());
        }

        int index = 0;

        for (T t : list) {
            result.get(index).add(t);
            index = (index + 1) % size;
        }

        return result;
    }

    static Map<String, String> getQueryParamsMap(URL url) throws UnsupportedEncodingException {

        if (url == null) {
            return Collections.EMPTY_MAP;
        }

        // Get Query part of the url
        String queryPart = url.getQuery();

        if (queryPart == null || queryPart.isEmpty()) {
            return Collections.EMPTY_MAP;
        }

        Map<String, String> queryParams = new HashMap<String, String>();

        String[] pairs = queryPart.split("&");
        for (String pair : pairs) {
            String[] keyValuePair = pair.split("=");

            queryParams.put(URLDecoder.decode(keyValuePair[0], StandardCharsets.UTF_8.name()),
                    URLDecoder.decode(keyValuePair[1], StandardCharsets.UTF_8.name()));
        }
        return queryParams;
    }


}
