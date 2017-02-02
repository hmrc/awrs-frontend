// Ensure countryCode.js is imported after jquery-ui.min
$(document).ready(function() {
    $('*[id*=addressCountry]').autocomplete( {
        source: countries,
        minLength: 2
    });
    // this overloads a method from jquery-ui.min for autocomplete to only match on "starts with".
    $.extend( $.ui.autocomplete, {
        escapeRegex: function( value ) {
            return value.replace( /[\-\[\]{}()*+?.,\\\^$|#\s]/g, "\\$&" );
        },
        filter: function( array, term ) {
            var matcher = new RegExp( "^" + $.ui.autocomplete.escapeRegex( term ), "i" );
            return $.grep( array, function( value ) {
                return matcher.test( value.label || value.value || value );
            });
        }
    });
});