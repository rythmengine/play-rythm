@**
 *  create a select element, with the correct options id valueProperty is set
 *  arg (required) name attribute of the generated select
 *  size (optional) size attribute of the generated select
 *  value (optional) selected element
 *  labelProperty (optional) item property used as option's body
 *  valueProperty (optional) item property used as option's value. id is used by default.
 *  #{select 'hotels', items:hotels, valueProperty:'id', labelProperty:'name'} #{/select}
 *@

@args String name, int size, Object value, String labelProperty, String valueProperty, Collection<?> items = Collections.EMPTY_LIST

@{
    if(s().isEmpty(name)) {
        throw new play.exceptions.TagInternalException("name attribute cannot be empty for select tag");
    }

    if(s().isEmpty(valueProperty)) valueProperty = "id";
    play.templates.TagContext.current().data.put("selected", value);

    String serializedAttrs  = play.templates.FastTags.serialize(__renderArgs, "size", "name", "items", "labelProperty", "value", "valueProperty");
}

<select name="@name" size="@size?:1" @serializedAttrs>
    @doBody()
    @for(item: items) {
        @{
            if (null != item && null != valueProperty && _hasBeanProperty(item, valueProperty)) valueProperty = _getBeanProperty(item, valueProperty);

            if (null != item && null != labelProperty && _hasBeanProperty(item, labelProperty)) labelProperty = s().escapeHtml(_getBeanProperty(item, labelProperty)).toString();
            else labelProperty = (null == item) ? "" : item.toString();
        }
        @play.option(valueProperty) {
            @msg(labelProperty)
        }
    }
</select>

