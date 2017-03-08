(function($) {
    var $lookupInput = $('input[id*="postcode"]'),
        $manualAddressLink = $('.address-container .font-small'),
        results = results || {},
        postcodeHistory = postcodeHistory || {},
        lookupRegex = /(([gG][iI][rR] {0,}0[aA]{2})|((([a-pr-uwyzA-PR-UWYZ][a-hk-yA-HK-Y]?[0-9][0-9]?)|(([a-pr-uwyzA-PR-UWYZ][0-9][a-hjkstuwA-HJKSTUW])|([a-pr-uwyzA-PR-UWYZ][a-hk-yA-HK-Y][0-9][abehmnprv-yABEHMNPRV-Y]))) {0,}[0-9][abd-hjlnp-uw-zABD-HJLNP-UW-Z]{2}))$/,
        northernIrelandRegex = /^B{1}T{1}/i,
        env = window.location.origin,
        lookUpPath = '/alcohol-wholesale-scheme/address-lookup?postcode=',
        auditPath = '/alcohol-wholesale-scheme/address-lookup',
        spinner,
        ajaxSuccess = true,
        data = {
            'addressAudits': []
        },
        detail = {},
        eventType;

    var leftArrow = 37, rightArrow = 39, downArrow = 40, upArrow = 38, enterKey = 13, spaceKey = 32;

    function getId(el) {
        var id = el.id;

        if (id.indexOf(".") >= 0) {
            id = id.replace(".", "\\.");
        }
        return id;
    }

    function updateAudits(auditConfig, fromAddress) {
        var addressContainer = auditConfig.addressContainer,
            eventType = auditConfig.eventType,
            index = auditConfig.index,
            uprn = auditConfig.uprn,
            submitting = auditConfig.submitting,
            id = addressContainer.id,
            $fields = $('#' + id).find('input:text'),
            fieldValues = [],
            fieldId = $('#' + id).find('input:text').attr('id'),
            addressId = fieldId.substr(0, fieldId.indexOf(".")),
            detail = detail || {};

        $fields.each(function() {
            fieldValues.push(this.value);
        });

        detail['auditPointId'] = addressId;
        detail['eventType'] = eventType;
        detail['uprn'] = uprn;
        // we do not want to update the from address when submitting the form so use address passed in
        if (!submitting) {
            detail.fromAddress = {};
            detail.fromAddress.addressLine1 = fieldValues[0];
            detail.fromAddress.addressLine2 = fieldValues[1];
            detail.fromAddress.addressLine3 = fieldValues[2];
            detail.fromAddress.addressLine4 = fieldValues[3];
            if (detail.eventType == 'internationalAddressSubmitted') {
                detail.fromAddress.addressCountry = fieldValues[5];
            } else {
                detail.fromAddress.postcode = fieldValues[4];
            }
        } else {
            detail.fromAddress = fromAddress;
        }

        detail.toAddress = {};
        detail.toAddress.addressLine1 = fieldValues[0];
        detail.toAddress.addressLine2 = fieldValues[1];
        detail.toAddress.addressLine3 = fieldValues[2];
        detail.toAddress.addressLine4 = fieldValues[3];
        if (detail.eventType == 'internationalAddressSubmitted') {
            detail.toAddress.addressCountry = fieldValues[5];
        } else {
            detail.toAddress.postcode = fieldValues[4];
        }

        data.addressAudits.splice(index, 1, detail);
    }

    $('.address-container').each(function(index) {
        updateAudits({
            addressContainer : this,
            eventType : $('#ukSupplier-no').is(':checked') ? 'internationalAddressSubmitted' : 'manualAddressSubmitted',
            index : index,
            uprn : '',
            submitting : false
        });
    });

    function filterData(data) {
        data.addresses = data.addresses.filter(function(value) {
            var valid = true;
            // check town is not longer than 35 chars
            if (value.address.town.length > 35) {
                return false;
            } else {
                // check address lines are not longer than 35 chars
                value.address.lines.map(function(line) {
                    // if > 35 chars, push index of parent object to array
                    if (line.length > 35) {
                        valid =  false;
                    }
                });
            }
            return valid;
        });
        results = data;
    }

    function buildOptions(data, num) {
        filterData(data);
        var address = data.addresses.length != 1 ? "addresses" : "address";
        var options = '<legend>' + data.addresses.length + ' ' + address + ' found...</legend>';
        data.addresses.map(function(results, index) {
            options += '<label id="result-' + num + 'choi' + index + '" class="block-label" for="result-' + num + 'choice' + index + '" value="' + index + '"><input class="postcode-lookup-results-entry" type="radio" id="result-' + num + 'choice' + index + '" name="res" value="' + index + '">' +
            results.address.lines.map(function(line, index) {
                return index == 0 ? line : ' ' + line;
            }) + ', ' +
            results.address.town + ', ' +
            results.address.postcode +
            '</label>';
            return options;
        });
        $('#result-' + num).html(options);
        $('#result-' + num +'choi0').addClass('selected add-focus').focus();
                var addressSize = data.addresses.length;
                $(dynamicListener(data, num, addressSize));
            }

            function dynamicListener(data, num, addressSize) {
                data.addresses.map(function(results, index) {
                   $('#result-' + num + 'choi' + index).on('keydown', function(e) {

                        blockLeftAndRightArrowNavigation(e);

                        if (e.which == downArrow && index < (addressSize - 1)) {
                            var nextIndex = index + 1;
                            var $this = $('#' + $(this).attr('id'));
                            $this.removeClass('selected add-focus');
                            $('#result-' + num + 'choi' + nextIndex).addClass('selected add-focus').focus();
                        }

                        if (e.which == upArrow && index > 0) {
                            var previousIndex = index - 1;
                            $('#' + $(this).attr('id')).removeClass('selected add-focus');
                            $('#result-' + num + 'choi'+previousIndex).addClass('selected add-focus').focus();
                        }
                    });
                    $('#result-' + num + 'choi' + index).on('keypress', function(e) {
                        e.preventDefault();
                        if (e.which == enterKey) {
                            var $this = $('#' + $(this).attr('id')),
                                num = spinner,
                                $parent = $('#address-' + num),
                                resultIndex = index,
                                results = data;
                            fillAddressFields($this, $parent, resultIndex, results);
                        }
                    });
                });
            }

            function fillAddressFields(elem, parent, resultIndex, results) {
                if (elem.val() != 'first') {
                    parent.find('input[id*="addressLine"]').val('');
                    parent.find('.address-lines').show();

                    uprn = results.addresses[resultIndex].id;

                    var townFieldNumber = results.addresses[resultIndex].address.lines.length + 1;

                    results.addresses[resultIndex].address.lines.map(function(line, index) {
                        parent.find('input[id$=addressLine' + (index + 1) + ']').val(line);
                    });

                    parent.find('input[id$=addressLine' + townFieldNumber + ']').val(results.addresses[resultIndex].address.town);
                    parent.find('input[id*="postcode"]').focus().val(results.addresses[resultIndex].address.postcode);

                    parent.find('.postcode-lookup').hide();

                    parent.find('.link-style').text('Look up address');

                    parent.each(function() {
                        updateAudits({
                            addressContainer : this,
                            eventType : 'postcodeAddressSubmitted',
                            index : resultIndex,
                            uprn : uprn,
                            submitting : false
                        });
                    });

                    clearResults(resultIndex);
                    $('.dropdown-menu').empty();
                    $('.postcode-lookup').show();
                    $('#address-' + resultIndex + ' input[id$="addressLine1"]').focus();
                }
    }

    function showResult(data, num) {
        buildOptions(data, num);
        if (data.addresses.length == 0) {
            showErrorMessage('No results found, check postcode and try again.', num);
            $(".dropdown-menu").hide();
        } else {
            $('#result-' + num).addClass('show').focus();
            $(".dropdown-menu").show();
        }
    }

    function clearResults(num) {
        var $result = $('#result-' + num);

        $result.removeClass('show').find('option').remove();
    }

    function showErrorMessage(message, num) {
        var $postcodeLookupWrapper = $('#postcode-lookup-button-' + num).parent('div');

        if ($postcodeLookupWrapper.hasClass('form-field--error')) {
            $postcodeLookupWrapper.find('.error-notification').text(message);
        }

        if (!$postcodeLookupWrapper.hasClass('form-field--error')) {
            $postcodeLookupWrapper.addClass('form-field--error');

            $postcodeLookupWrapper.find('label').prepend(
                '<span class="error-notification" role="tooltip" data-journey="search-page:error:additionalAddress.postcode">' + message + '</span>'
            );
        }

        clearResults(num);
        $postcodeLookupWrapper.find('input[type="text"]').focus();
    }

    function hideErrorMessage(num) {
        var $address = $('#postcode-lookup-button-' + num).parents('#address-' + num);

        if ($address.find('div.form-field').hasClass('form-field--error')) {
            $address.find('.error-notification').remove();
        }
        if ($address.data("attribute") == "panel-indent") {
            $address.find('div.form-field').removeClass('form-field--error').addClass('panel-indent');
        } else {
            $address.find('div.form-field').removeClass('form-field--error');
        }
    }

    function clearAddress(num) {
        var $address = $('#postcode-lookup-button-' + num).parents('#address-' + num);

        $address.find('.address-lines input').val('');
    }

    function doError(jqXHR, num) {
        if (jqXHR.status == 0) {
            showErrorMessage('Your session has timed out.', num);
        }
        if (jqXHR.status == 400) {
            showErrorMessage('No results found, check postcode and try again.', num);
        }
        if (jqXHR.status == 404) {
            showErrorMessage('Not found. Unknown URI accessed.', num);
        }
        if (jqXHR.status == 405) {
            showErrorMessage('Bad method. Unacceptable method used.', num);
        }
        if (jqXHR.status == 500) {
            showErrorMessage('Postcode Lookup is currently unavailable. Enter your address manually or try again later.', num);
        }
    }

    function validation(postcode, url, num, id) {
        var valid = true;
        // check not empty
        if (postcode == '') {
            valid = false;
            showErrorMessage('Postcode must not be empty', num);
        }

        // check for illegal chars
        if (valid && !postcode.match(lookupRegex)) {
            valid = false;
            showErrorMessage('Postcode invalid', num);
        }

        // exclude NI postcodes
        if (valid && postcode.match(northernIrelandRegex)) {
            valid = false;
            showErrorMessage('Currently, we cannot return any results for a Northern Ireland postcode', num);
        }

        // only do ajax call when postcode is entered and changes and passes regex
        if (valid) {
            if(ajaxSuccess){
                hideErrorMessage(num);
            }
            searchAddress(url, num);
        }
    }

    function searchAddress(url, num) {
        spinner = num;

        $.ajax({
            type: 'GET',
            url: url,
            dataType: "json",
            success: function(data) {
                ajaxSuccess = true;
                showResult(data, num);
            },
            error: function(jqXHR) {
                ajaxSuccess = false;
                doError(jqXHR, num);
            },
            headers: {"X-Hmrc-Origin": "awrs"}
        });
    }
    
    function auditEvents(url, data, form) {
        $.ajax({
            type: 'POST',
            url: url,
            data: JSON.stringify(data),
            contentType: 'application/json',
            processData: false,
            success: function() {
                form.submit();
            },
            error: function() {
                form.submit();
            },
            headers: {"X-Hmrc-Origin": "awrs"}
        });
    }

    function manualLookupHandler(el, num) {
        var $this = el;

        postcodeHistory = {};

        $this.text() == 'Enter address manually' ? $this.text('Look up address') : $this.text('Enter address manually');

        if ($this.text() == 'Look up address') {
            $('#address-' + num + ' .address-lines').show();
            $('#address-' + num + ' input[id$="addressLine1"]').focus();
            $('#result-' + spinner + '_field').attr('aria-hidden', 'true');
            $('#postcode-lookup-button-' + num).hide();
        } else {
            $('#address-' + num + ' .address-lines').hide();
            $('#postcode-lookup-button-' + num).show();
            $('#address-' + num + ' input[id$="postcode"]').focus();
        }

        clearResults(num);
    }

    function blockLeftAndRightArrowNavigation(e) {
        if (e.which == leftArrow || e.which == rightArrow) {
            e.preventDefault();
        }
    }

    $manualAddressLink.show();
    $('.postcode-lookup').show();

    // load state
    $('.address-container').each(function() {
        var id = this.id,
            num = id.substr(id.length - 1),
            $this = $('#' + this.id),
            $addressLines = $($this).find('.address-lines'),
            $manualLookupSpan = $($this).find('#manual-address-span-' + num),
            $postcodeLookupButton = $($this).find('#postcode-lookup-button-' + num);

        // if address lines pull back data, show them and set lookup accordingly
        if ($addressLines.find('input').length == $addressLines.find('input[value=""]').length && $addressLines.find('div.form-field--error').length == 0) {
            $addressLines.hide();
            $manualLookupSpan.text('Enter address manually');
            $postcodeLookupButton.show();
        }
        else {
            $addressLines.show();
            $manualLookupSpan.text('Look up address');
            $postcodeLookupButton.hide();
        }

    });

    $(document).ajaxStart(function() {
        $('#spinner-' + spinner).show();
        $('#result-' + spinner + '_field').removeAttr('aria-hidden');
    }).ajaxStop(function() {
        $('#spinner-' + spinner).hide();
    });

    $lookupInput.on('focus', function() {
        $('#' + getId(this)).next('a').filter(':visible').addClass('postcode-lookup-color-change');
    });

    $lookupInput.on('focusout', function() {
        $('#' + getId(this)).next('a').filter(':visible').removeClass('postcode-lookup-color-change');
    });

    $('input[id*="postcode"]').on('keydown, keyup, keypress', function(e) {
        var $this = $('#' + getId(this));

        if (e.which == enterKey) {
            e.preventDefault();
            $this.next('a').filter(':visible').click();
            return false;
        }
    });

    $('.link-style').on('click', function() {
        var id = this.id,
            $this = $('#' + this.id),
            num = id.substr(id.length - 1);

        hideErrorMessage(num);
        clearAddress(num);
        manualLookupHandler($this, num);
    });

    $('.link-style').on('keydown, keyup, keypress', function(e) {
        var id = this.id,
            $this = $('#' + this.id),
            num = id.substr(id.length - 1);

        if (e.which == spaceKey || e.which == enterKey) {
            e.preventDefault();
            hideErrorMessage(num);
            clearAddress(num);
            manualLookupHandler($this, num);
        }
    });

    // this function is added to allow the post code to be searched again if say the user accidentally hide the form
    // section and cleared the data
    // in this model we allow the user to perform additional ajax calls as long as there has been a change in the search
    // input box
    $('input[id*="postcode"]').on('input', function() {
        postcodeHistory = {};
    });

    $('.postcode-lookup').on('click', function() {
        var postcode = $(this).prev().val().replace(/\s/g,''),
            url = env + lookUpPath + postcode,
            id = getId(this),
            num = id.substr(id.length -1);

        validation(postcode, url, num, id);
    });

    $('.postcode-lookup').on('keydown, keyup, keypress', function(e) {
        var postcode = $(this).prev().val().replace(/\s/g,''),
            url = env + lookUpPath + postcode,
            id = getId(this),
            num = this.id.substr(this.id.length -1);

        if (e.which == spaceKey || e.which == enterKey) {
            validation(postcode, url, num, id);
            return false;
        }
    });

    $('.postcode-lookup-results').on('click', function() {
        var $this = $('#' + this.id),
            num = spinner,
            $parent = $('#address-' + num),
            resultIndex = this.value;
            fillAddressFields($this, $parent, resultIndex, results)
    });

    $('form').submit(function(e){
        e.preventDefault();
        var form = this;

        $('.address-container').each(function(index) {
            var audit = data.addressAudits[index];

            updateAudits({
                addressContainer : this,
                eventType : $('#ukSupplier-no').is(':checked') ? 'internationalAddressSubmitted' : audit.eventType,
                index : index,
                uprn : audit.uprn,
                submitting : true
            }, audit.fromAddress);
        });

        auditEvents(env + auditPath, data, form);
    });
})(jQuery);