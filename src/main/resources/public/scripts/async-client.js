(() => {
    'use strict';

    const serviceCards = document.querySelectorAll('[data-service-card]');
    const resultsList = document.querySelector('#results-list');
    const errorsList = document.querySelector('#errors-list');
    const resultsEmpty = document.querySelector('#results-empty');
    const errorsEmpty = document.querySelector('#errors-empty');
    const resultCount = document.querySelector('#result-count');
    const errorCount = document.querySelector('#error-count');

    if (!serviceCards.length || !resultsList || !errorsList || !resultsEmpty || !errorsEmpty || !resultCount || !errorCount) {
        return;
    }

    const serviceDefinitions = {
        greeting: {
            label: 'Saludo',
            path: '/hello',
            buildParams: (formData) => ({
                name: String(formData.get('name') || '').trim()
            }),
            validate: (params) => params.name
                ? ''
                : 'Escribe un nombre antes de solicitar el saludo.'
        },
        square: {
            label: 'Cuadrado',
            path: '/square',
            buildParams: (formData) => ({
                value: String(formData.get('value') || '').trim()
            }),
            validate: (params) => {
                if (!params.value) {
                    return 'Escribe un número antes de calcular su cuadrado.';
                }

                if (!Number.isFinite(Number(params.value))) {
                    return 'El valor debe ser un número válido.';
                }

                return '';
            }
        },
        'server-time': {
            label: 'Hora del servidor',
            path: '/time',
            buildParams: () => ({}),
            validate: () => ''
        },
        health: {
            label: 'Estado del servidor',
            path: '/health',
            buildParams: () => ({}),
            validate: () => ''
        }
    };

    class HttpResponseError extends Error {
        constructor(status, message) {
            super(message);
            this.name = 'HttpResponseError';
            this.status = status;
        }
    }

    const buildServiceUrl = (path, params) => {
        const url = new URL(path, window.location.origin);

        Object.entries(params).forEach(([key, value]) => {
            if (value !== '') {
                url.searchParams.set(key, value);
            }
        });

        return url;
    };

    const readJsonResponse = async (response) => {
        try {
            return await response.json();
        } catch (error) {
            throw new HttpResponseError(
                response.status,
                'El servidor respondió, pero el contenido no tiene un JSON válido.'
            );
        }
    };

    const getServerMessage = (payload, fallback) => {
        if (payload && typeof payload.mensaje === 'string') {
            return payload.mensaje;
        }

        return fallback;
    };

    const setCardLoading = (card, button, statusElement, isLoading) => {
        card.setAttribute('aria-busy', String(isLoading));
        button.disabled = isLoading;
        button.textContent = isLoading ? 'Cargando…' : button.dataset.defaultLabel;

        if (isLoading) {
            statusElement.dataset.state = 'loading';
            statusElement.textContent = 'Cargando respuesta… La página sigue disponible.';
        }
    };

    const clearEmptyState = (emptyElement) => {
        emptyElement.hidden = true;
    };

    const updateCounter = (element, list, emptyElement) => {
        const itemCount = list.querySelectorAll('.feedback-item').length;
        element.textContent = String(itemCount);
        emptyElement.hidden = itemCount > 0;
    };

    const createTextElement = (tagName, className, text) => {
        const element = document.createElement(tagName);
        element.className = className;
        element.textContent = text;
        return element;
    };

    const addSuccessResult = (service, url, payload) => {
        clearEmptyState(resultsEmpty);

        const item = document.createElement('article');
        item.className = 'feedback-item';

        const heading = document.createElement('div');
        heading.className = 'feedback-item__heading';
        heading.appendChild(createTextElement('h3', '', service.label));
        heading.appendChild(createTextElement('span', 'feedback-item__time', new Date().toLocaleTimeString('es-CO')));

        item.appendChild(heading);
        item.appendChild(createTextElement('span', 'feedback-item__url', url.toString()));
        item.appendChild(createTextElement('p', 'feedback-item__data', formatPayload(payload)));

        resultsList.prepend(item);
        updateCounter(resultCount, resultsList, resultsEmpty);
    };

    const addErrorMessage = (title, message, detail) => {
        clearEmptyState(errorsEmpty);

        const item = document.createElement('article');
        item.className = 'feedback-item feedback-item--error';

        const heading = document.createElement('div');
        heading.className = 'feedback-item__heading';
        heading.appendChild(createTextElement('h3', '', title));
        heading.appendChild(createTextElement('span', 'feedback-item__time', new Date().toLocaleTimeString('es-CO')));

        item.appendChild(heading);
        item.appendChild(createTextElement('p', 'feedback-item__message', message));

        if (detail) {
            item.appendChild(createTextElement('p', 'feedback-item__message', detail));
        }

        errorsList.prepend(item);
        updateCounter(errorCount, errorsList, errorsEmpty);
    };

    const formatPayload = (payload) => {
        if (payload && typeof payload.mensaje === 'string') {
            return payload.mensaje;
        }

        if (payload && typeof payload.status === 'string') {
            return `Estado: ${payload.status}`;
        }

        if (payload && Object.prototype.hasOwnProperty.call(payload, 'value') && Object.prototype.hasOwnProperty.call(payload, 'square')) {
            return `Valor: ${payload.value}\nCuadrado: ${payload.square}`;
        }

        if (payload && typeof payload.serverTime === 'string') {
            return `Hora del servidor: ${payload.serverTime}`;
        }

        return JSON.stringify(payload);
    };

    const requestService = async (service, params) => {
        const url = buildServiceUrl(service.path, params);
        const response = await fetch(url, {
            method: 'GET'
        });

        if (!response.ok) {
            let errorMessage = `El servidor respondió con HTTP ${response.status}.`;

            try {
                const contentType = response.headers.get('Content-Type') || '';
                if (contentType.includes('application/json')) {
                    const errorPayload = await response.json();
                    errorMessage = getServerMessage(errorPayload, errorMessage);
                } else {
                    const text = await response.text();
                    if (text) errorMessage = text;
                }
            } catch (error) {
                // si falla el parseo, queda el mensaje default
            }

            throw new HttpResponseError(response.status, errorMessage);
        }

        const contentType = response.headers.get('Content-Type') || '';
        let payload;

        if (contentType.includes('application/json')) {
            payload = await readJsonResponse(response);
        } else {
            const text = await response.text();
            payload = { mensaje: text };
        }

        return { url, payload };
    };

    serviceCards.forEach((card) => {
        const serviceName = card.dataset.serviceCard;
        const service = serviceDefinitions[serviceName];
        const form = card.querySelector('[data-service-form]');
        const button = form ? form.querySelector('button[type="submit"]') : null;
        const statusElement = card.querySelector('[data-service-status]');

        if (!service || !form || !button || !statusElement) {
            return;
        }

        button.dataset.defaultLabel = button.textContent.trim();

        form.addEventListener('submit', async (event) => {
            event.preventDefault();
            statusElement.textContent = '';
            delete statusElement.dataset.state;

            const formData = new FormData(form);
            const params = service.buildParams(formData);
            const validationMessage = service.validate(params);

            if (validationMessage) {
                statusElement.dataset.state = 'error';
                statusElement.textContent = validationMessage;
                addErrorMessage(`Entrada inválida · ${service.label}`, validationMessage);
                return;
            }

            setCardLoading(card, button, statusElement, true);

            try {
                const { url, payload } = await requestService(service, params);
                addSuccessResult(service, url, payload);
                statusElement.dataset.state = 'success';
                statusElement.textContent = 'Respuesta recibida correctamente.';
            } catch (error) {
                if (error instanceof HttpResponseError) {
                    const message = error.status === 400
                        ? 'La solicitud fue rechazada porque faltan datos o contienen un formato inválido.'
                        : 'El servidor no pudo completar la solicitud.';
                    addErrorMessage(`Error HTTP ${error.status} · ${service.label}`, message, error.message);
                    statusElement.dataset.state = 'error';
                    statusElement.textContent = `Error HTTP ${error.status}.`;
                } else {
                    addErrorMessage(
                        `Fallo de red · ${service.label}`,
                        'No fue posible comunicarse con el servidor. Verifica que HTTPServer esté ejecutándose en este origen.',
                        'La solicitud no recibió una respuesta HTTP.'
                    );
                    statusElement.dataset.state = 'error';
                    statusElement.textContent = 'Fallo de red.';
                }
            } finally {
                setCardLoading(card, button, statusElement, false);
                if (statusElement.dataset.state === 'success') {
                    statusElement.textContent = 'Respuesta recibida correctamente.';
                } else if (statusElement.dataset.state === 'error') {
                    statusElement.textContent = statusElement.textContent || 'La solicitud terminó con error.';
                }
            }
        });
    });
})();