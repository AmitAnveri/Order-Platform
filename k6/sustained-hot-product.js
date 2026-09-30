import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const createdOrders = new Counter('created_orders');
const unexpectedResponses = new Counter('unexpected_responses');

export const options = {
    thresholds: {
        checks: ['rate==1'],
    },
    scenarios: {
        hot_product: {
            executor: 'constant-vus',
            vus: Number(__ENV.VUS || 10),
            duration: '30s',
        },
    },

    summaryTrendStats: [
        'avg',
        'med',
        'p(90)',
        'p(95)',
        'p(99)',
        'max',
    ],
};

export default function () {
    const productId = Number(__ENV.PRODUCT_ID || 1);

    const payload = JSON.stringify({
        userId: `load-test-user-${__VU}`,
        items: [
            {
                productId: productId,
                quantity: 1,
            },
        ],
    });

    const response = http.post(
        __ENV.ORDER_URL,
        payload,
        {
            headers: {
                'Content-Type': 'application/json',
            },
        }
    );

    const created = check(response, {
        'order created': (r) => r.status === 201,
    });

    if (created) {
        createdOrders.add(1);
    } else {
        unexpectedResponses.add(1);

        console.log(
            `VU=${__VU} status=${response.status} body=${response.body}`
        );
    }
}
